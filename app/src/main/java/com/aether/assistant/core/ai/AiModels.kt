package com.aether.assistant.core.ai

enum class Role { USER, ASSISTANT, SYSTEM }

data class ChatMessage(val role: Role, val content: String, val timestamp: Long = System.currentTimeMillis())

data class AiRequest(
    val messages: List<ChatMessage>,
    val base64Images: List<String> = emptyList(),
    val temperature: Double = 0.4,
    val maxTokens: Int = 800
)

data class AiResponse(
    val text: String,
    val engineLabel: String,
    val usedOnline: Boolean,
    val latencyMs: Long
)

interface AiEngine {
    suspend fun generate(request: AiRequest): Result<AiResponse>
    fun label(): String
    fun usesNetwork(): Boolean
}
