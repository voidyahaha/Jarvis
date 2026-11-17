package com.aether.assistant.core.ai

import com.aether.assistant.core.settings.AiEndpoint
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * OpenAI-compatible client. Works with any user-configured endpoint that speaks
 * /chat/completions — including a local llama.cpp / Ollama server on 127.0.0.1.
 * No key is hard-coded; the token comes from encrypted app settings.
 */
class OnlineEngine(private val endpoint: AiEndpoint) : AiEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownElements = true }

    override fun label(): String = if (isLocalAddress()) "🟢 Offline AI · local server (${endpoint.name})"
                                  else "🔵 Online AI · ${endpoint.name}"

    override fun usesNetwork(): Boolean = !isLocalAddress()

    private fun isLocalAddress(): Boolean =
        endpoint.baseUrl.contains("127.0.0.1") || endpoint.baseUrl.contains("localhost") ||
            endpoint.baseUrl.contains("10.0.2.2")

    override suspend fun generate(request: AiRequest): Result<AiResponse> = runCatching {
        val started = System.currentTimeMillis()

        val messagesArray = buildJsonArray {
            request.messages.forEach { m ->
                add(buildJsonObject {
                    put("role", when (m.role) {
                        Role.SYSTEM -> "system"
                        Role.USER -> "user"
                        Role.ASSISTANT -> "assistant"
                    })
                    if (request.base64Images.isNotEmpty() && m.role == Role.USER) {
                        val content = buildJsonArray {
                            add(buildJsonObject {
                                put("type", "text")
                                put("text", m.content)
                            })
                            request.base64Images.forEach { b64 ->
                                add(buildJsonObject {
                                    put("type", "image_url")
                                    put("image_url", buildJsonObject {
                                        put("url", "data:image/jpeg;base64,$b64")
                                    })
                                })
                            }
                        }
                        put("content", content)
                    } else {
                        put("content", m.content)
                    }
                })
            }
        }

        val bodyJson = buildJsonObject {
            put("model", endpoint.model)
            put("messages", messagesArray)
            put("temperature", request.temperature)
            put("max_tokens", request.maxTokens)
            put("stream", false)
        }

        val url = endpoint.baseUrl.trimEnd('/') + "/chat/completions"
        val req = Request.Builder()
            .url(url)
            .apply { if (endpoint.token.isNotBlank()) header("Authorization", "Bearer ${endpoint.token}") }
            .header("Content-Type", "application/json")
            .post(json.encodeToString(JsonObject.serializer(), bodyJson).toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(req).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("AI endpoint ${resp.code}: ${raw.take(240)}")
            val obj = json.parseToJsonElement(raw) as JsonObject
            val choices = obj["choices"] as? JsonArray ?: error("No choices in AI response")
            val first = choices.firstOrNull() as? JsonObject ?: error("Empty AI response")
            val msg = first["message"] as? JsonObject ?: error("No message in response")
            val text = (msg["content"] as? JsonPrimitive)?.content.orEmpty()
            AiResponse(
                text = text.ifBlank { "(empty response)" },
                engineLabel = label(),
                usedOnline = usesNetwork(),
                latencyMs = System.currentTimeMillis() - started
            )
        }
    }
}
