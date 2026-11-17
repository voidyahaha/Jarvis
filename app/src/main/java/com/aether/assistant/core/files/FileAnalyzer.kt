package com.aether.assistant.core.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream
import kotlin.math.min

data class FileAnalysis(
    val name: String,
    val mime: String,
    val sizeBytes: Long,
    val summary: String,
    val details: List<String>,
    val usedOnline: Boolean = false
)

/** Local-first file analysis. Large files report progress instead of freezing. */
class FileAnalyzer(private val context: Context) {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun analyze(uri: Uri, onProgress: (String) -> Unit): Result<FileAnalysis> = withContext(Dispatchers.IO) {
        runCatching {
            val cr = context.contentResolver
            val name = cr.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) c.getString(idx) else "file"
            } ?: "file"
            val size = cr.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
            val mime = cr.getType(uri) ?: "application/octet-stream"
            onProgress("Reading $name (${size / 1024} KB)…")

            val (summary, details) = when {
                mime.startsWith("image/") -> analyzeImage(uri, onProgress)
                mime == "application/pdf" -> analyzePdf(uri, onProgress)
                mime.startsWith("audio/") || mime.startsWith("video/") -> analyzeMedia(uri, name)
                mime.contains("zip") -> analyzeZip(uri, onProgress)
                mime.contains("csv") || name.endsWith(".csv", true) -> analyzeCsv(uri, onProgress)
                mime.startsWith("text/") || isCode(name) -> analyzeText(uri, onProgress)
                else -> "Unsupported type locally" to listOf(
                    "MIME: $mime",
                    "Try converting to text/PDF/image, or enable Online Files in Settings."
                )
            }
            FileAnalysis(name, mime, size, summary, details)
        }
    }

    private fun isCode(name: String) = listOf(
        ".kt", ".java", ".py", ".js", ".ts", ".tsx", ".jsx", ".html", ".css", ".xml",
        ".json", ".sh", ".c", ".cpp", ".h", ".go", ".rs", ".php", ".rb", ".sql"
    ).any { name.endsWith(it, true) }

    private suspend fun analyzeImage(uri: Uri, onProgress: (String) -> Unit): Pair<String, List<String>> {
        onProgress("Running on-device OCR…")
        val sourceBitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            android.graphics.ImageDecoder.decodeBitmap(
                android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
            )
        } else {
            @Suppress("DEPRECATION")
            BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                ?: return "Could not decode image" to emptyList()
        }
        val bitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, false)
        val result = com.google.android.gms.tasks.Tasks.await(
            textRecognizer.process(InputImage.fromBitmap(bitmap, 0))
        )
        return "Image analyzed on-device" to listOf(
            "Dimensions: ${bitmap.width}×${bitmap.height}",
            "Text found: ${result.text.take(1500).ifBlank { "(none)" }}"
        )
    }

    private suspend fun analyzePdf(uri: Uri, onProgress: (String) -> Unit): Pair<String, List<String>> {
        val fd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: return "Could not open PDF" to emptyList()
        return fd.use { pfd: ParcelFileDescriptor ->
            val renderer = PdfRenderer(pfd)
            val texts = mutableListOf<String>()
            val pageCount = min(renderer.pageCount, 5)
            for (i in 0 until pageCount) {
                onProgress("OCR page ${i + 1}/$pageCount…")
                val page = renderer.openPage(i)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                val res = com.google.android.gms.tasks.Tasks.await(
                    textRecognizer.process(InputImage.fromBitmap(bitmap, 0))
                )
                texts += "Page ${i + 1}: ${res.text.take(600)}"
                page.close()
                bitmap.recycle()
            }
            renderer.close()
            "Processed $pageCount page(s) locally" to texts
        }
    }

    private fun analyzeMedia(uri: Uri, name: String): Pair<String, List<String>> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) ?: "?"
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: name
            "Metadata read locally (transcription requires a local speech model)" to
                listOf("Title: $title", "Duration: ${dur}ms")
        } finally {
            retriever.release()
        }
    }

    private fun analyzeZip(uri: Uri, onProgress: (String) -> Unit): Pair<String, List<String>> {
        val entries = mutableListOf<String>()
        ZipInputStream(context.contentResolver.openInputStream(uri)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null && entries.size < 120) {
                entries += "${if (entry.isDirectory) "📁" else "📄"} ${entry.name}"
                onProgress("Listing ${entries.size}…")
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return "ZIP listed locally (${entries.size} entries)" to entries
    }

    private suspend fun analyzeCsv(uri: Uri, onProgress: (String) -> Unit): Pair<String, List<String>> {
        val lines = context.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input)).readLines().take(12)
        } ?: emptyList()
        onProgress("Reading rows…")
        val cols = lines.firstOrNull()?.split(",")?.size ?: 0
        return "CSV preview ready" to listOf("Columns: $cols", "First rows:", *lines.take(6).toTypedArray())
    }

    private suspend fun analyzeText(uri: Uri, onProgress: (String) -> Unit): Pair<String, List<String>> {
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input)).readText()
        } ?: ""
        onProgress("Analyzing ${text.length} characters…")
        val lines = text.lines()
        val imports = lines.filter { it.trim().startsWith("import ") || it.trim().startsWith("#include") }.take(15)
        val functions = lines.filter {
            it.contains("fun ") || it.contains("function ") || it.contains("def ") ||
                it.contains("func ") || it.contains("public ")
        }.take(15)
        return "Source/text analyzed locally" to listOf(
            "Lines: ${lines.size}",
            "Characters: ${text.length}",
            "Imports/includes: ${imports.joinToString("; ").ifBlank { "(none)" }}",
            "Functions/defs: ${functions.joinToString("; ").ifBlank { "(none)" }}",
            "Preview:",
            text.take(1200)
        )
    }
}
