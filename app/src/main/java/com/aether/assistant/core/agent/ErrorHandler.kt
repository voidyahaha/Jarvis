package com.aether.assistant.core.agent

import com.aether.assistant.core.ai.AiRequest
import com.aether.assistant.core.ai.AiManager
import com.aether.assistant.core.ai.ChatMessage
import com.aether.assistant.core.ai.LocalEngine
import com.aether.assistant.core.ai.Role

/** Error handler → safe alternatives instead of crashing or retrying forever. */
class ErrorHandler(
    private val aiManager: AiManager,
    private val localEngine: LocalEngine
) {
    suspend fun recover(action: AgentAction, error: Throwable, attempt: Int): Recovery {
        if (attempt >= 2) {
            return Recovery(false, "Stopped after 2 attempts: ${error.message ?: "unknown error"}")
        }
        return when (action) {
            is AgentAction.Chat -> {
                // Online AI failed → genuinely fall back to the offline engine.
                val fallback = localEngine.generate(
                    AiRequest(listOf(ChatMessage(Role.USER, action.text)))
                ).getOrNull()
                if (fallback != null) Recovery(true, fallback.text + "\n\n(Offline fallback used.)")
                else Recovery(false, "Both online and offline AI failed.")
            }
            is AgentAction.Click, is AgentAction.TypeText -> Recovery(
                false,
                "Automation failed: ${error.message}. Enable Accessibility or guide me manually."
            )
            is AgentAction.OpenApp -> Recovery(
                false,
                "Could not open app: ${error.message}. Check the app name or install it."
            )
            else -> Recovery(false, "Recoverable issue: ${error.message}")
        }
    }

    data class Recovery(val recovered: Boolean, val detail: String)
}
