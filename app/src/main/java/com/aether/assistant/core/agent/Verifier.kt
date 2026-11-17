package com.aether.assistant.core.agent

/** Verifier → checks that an action really produced a usable result. */
class Verifier {
    fun verify(action: AgentAction, result: Result<String>): Verification {
        val text = result.getOrNull()
        if (result.isFailure || text.isNullOrBlank()) {
            return Verification(false, "Action returned no usable result")
        }
        if (text.contains("error", true) && text.length < 120) {
            return Verification(false, text)
        }
        return when (action) {
            is AgentAction.OpenApp -> Verification(
                text.startsWith("Opened"),
                if (text.startsWith("Opened")) "App launch confirmed" else text
            )
            is AgentAction.Click, is AgentAction.TypeText, AgentAction.Scroll -> Verification(
                !text.contains("could not", true) && !text.contains("failed", true),
                text
            )
            else -> Verification(true, "OK")
        }
    }

    data class Verification(val ok: Boolean, val detail: String)
}
