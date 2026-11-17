package com.aether.assistant.core.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.util.Base64
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.ByteArrayOutputStream

data class VisionInsight(
    val text: String = "",
    val barcodes: List<String> = emptyList(),
    val labels: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

/** CameraX + on-device ML Kit. No frame is stored or uploaded automatically. */
class CameraAnalyzer(private val context: Context) {

    private val _insight = MutableStateFlow(VisionInsight())
    val insight: StateFlow<VisionInsight> = _insight

    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val barcodeScanner = BarcodeScanning.getClient()
    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    fun bind(previewView: PreviewView, owner: LifecycleOwner, onPermissionMissing: () -> Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                cameraProvider = future.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(ContextCompat.getMainExecutor(context)) { proxy -> analyze(proxy) } }

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    owner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                    analysis
                )
                _active.value = true
            } catch (e: Exception) {
                _active.value = false
                onPermissionMissing()
                Toast.makeText(context, "Camera unavailable: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun analyze(proxy: ImageProxy) {
        val media = proxy.image
        if (media == null) {
            proxy.close()
            return
        }
        val image = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
        textRecognizer.process(image)
            .addOnSuccessListener { result ->
                _insight.value = _insight.value.copy(text = result.text)
            }
            .addOnFailureListener { /* keep last good result */ }
            .addOnCompleteListener {
                barcodeScanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        _insight.value = _insight.value.copy(
                            barcodes = barcodes.mapNotNull { it.rawValue }.distinct()
                        )
                    }
                    .addOnCompleteListener {
                        labeler.process(image)
                            .addOnSuccessListener { labels ->
                                _insight.value = _insight.value.copy(
                                    labels = labels.sortedByDescending { it.confidence }
                                        .take(5).map { it.text }
                                )
                            }
                            .addOnCompleteListener { proxy.close() }
                    }
            }
    }

    /** Capture a JPEG and return it as base64 for optional AI vision. Never auto-uploaded. */
    fun captureBase64(onResult: (String?) -> Unit) {
        val capture = imageCapture ?: run { onResult(null); return }
        val file = java.io.File.createTempFile("aether_vision", ".jpg", context.cacheDir)
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        capture.takePicture(options, ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val bytes = file.readBytes()
                    onResult(Base64.encodeToString(bytes, Base64.NO_WRAP))
                }
                override fun onError(exception: ImageCaptureException) {
                    onResult(null)
                    Toast.makeText(context, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    fun close() {
        cameraProvider?.unbindAll()
        textRecognizer.close()
        barcodeScanner.close()
        labeler.close()
        _active.value = false
    }
}
