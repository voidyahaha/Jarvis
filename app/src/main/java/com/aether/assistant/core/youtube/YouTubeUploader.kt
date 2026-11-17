package com.aether.assistant.core.youtube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class UploadRequest(
    val videoFile: File,
    val title: String,
    val description: String,
    val tags: List<String> = emptyList(),
    val publishAtIso: String? = null,
    val makePublic: Boolean = true
)

/**
 * YouTube Data API upload using a user-supplied OAuth access token.
 * No credentials are bundled. Scheduling uses the official `publishAt` field.
 */
class YouTubeUploader {
    private val client = OkHttpClient.Builder()
        .callTimeout(5, TimeUnit.MINUTES)
        .build()

    suspend fun upload(req: UploadRequest, accessToken: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(accessToken.isNotBlank()) {
                "Add your YouTube OAuth access token in Settings first. Aether never bundles credentials."
            }
            require(req.videoFile.exists()) { "Video file not found" }

            val snippet = JSONObject().apply {
                put("title", req.title.take(100))
                put("description", req.description.take(5000))
                put("tags", org.json.JSONArray(req.tags.take(10)))
                put("categoryId", "22")
            }
            val status = JSONObject().apply {
                put("privacyStatus", if (req.publishAtIso != null) "private" else if (req.makePublic) "public" else "unlisted")
                put("selfDeclaredMadeForKids", false)
                if (req.publishAtIso != null) put("publishAt", req.publishAtIso)
            }
            val metadata = JSONObject().apply {
                put("snippet", snippet)
                put("status", status)
            }.toString()

            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("metadata", "metadata.json",
                    okhttp3.RequestBody.create("application/json; charset=UTF-8".toMediaType(), metadata))
                .addFormDataPart("video", req.videoFile.name,
                    req.videoFile.asRequestBody("video/*".toMediaType()))
                .build()

            val url = "https://www.googleapis.com/upload/youtube/v3/videos?part=snippet,status&uploadType=multipart"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $accessToken")
                .post(body)
                .build()

            client.newCall(request).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("YouTube API ${resp.code}: ${raw.take(300)}")
                val id = JSONObject(raw).optString("id")
                "Uploaded${if (req.publishAtIso != null) " and scheduled" else ""}: $id"
            }
        }
    }

    companion object {
        fun isoFrom(millis: Long): String {
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            return fmt.format(Date(millis))
        }
    }
}
