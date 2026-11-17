package com.aether.assistant.core.ai

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import android.widget.Toast
import com.aether.assistant.core.util.containsAny
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Genuine offline engine. Two layers:
 *  1) Deterministic intent router for real device actions + quick knowledge.
 *  2) Optional on-device HTTP model (user points Settings to 127.0.0.1:8080)
 *     handled by OnlineEngine; this class gives a useful, honest fallback when
 *     no local model server is configured.
 */
class LocalEngine(private val context: Context) : AiEngine {

    override fun label() = "🟢 Offline AI · on-device commands"
    override fun usesNetwork() = false

    override suspend fun generate(request: AiRequest): Result<AiResponse> = withContext(Dispatchers.Default) {
        val started = System.currentTimeMillis()
        runCatching {
            val text = request.messages.lastOrNull { it.role == Role.USER }?.content.orEmpty()
            val reply = offlineReply(text)
            AiResponse(reply, label(), false, System.currentTimeMillis() - started)
        }
    }

    private fun offlineReply(input: String): String {
        val lower = input.lowercase()
        return when {
            lower.containsAny("open", "launch", "start") && lower.containsAny("camera") ->
                "Opening camera. Say 'close' or tap back to return."

            lower.containsAny("flashlight", "torch") ->
                "I can toggle the flashlight through the device action layer. Use the ⚡ quick chip or say 'flashlight on'."

            lower.containsAny("alarm", "timer") ->
                "I can create an alarm/timer through Android's official AlarmClock intent."

            lower.containsAny("volume", "silent", "mute") ->
                "Volume control is available through the device action layer and Android's audio manager."

            lower.containsAny("remember", "note this", "save this") ->
                "Tell me 'remember: <text>' and I'll store it locally in encrypted memory."

            lower.containsAny("what can you do", "help", "features") ->
                HELP_TEXT

            lower.containsAny("who made you", "your name") ->
                "I'm Aether, a local-first Android assistant. My source code and logic are in this project."

            lower.containsAny("online", "internet", "cloud") ->
                "Online AI is optional. Switch modes in Settings; I never send data online without that."

            lower.isBlank() -> "I'm listening."

            else -> "I understood: \"${input.take(120)}\".\n\nI'm running in **offline command mode**. For open-ended reasoning, configure a local model server (e.g. llama.cpp on 127.0.0.1:8080) or switch to Online AI in Settings."
        }
    }

    companion object {
        const val HELP_TEXT = """I can help with:
• Voice chat, TTS, and hands-free mode
• Open apps, set alarms/timers, flashlight, volume
• Screen reading + tap/type/scroll (needs Accessibility)
• Camera text/label/QR understanding (needs Camera)
• File analysis (PDF, images, ZIP, code, CSV)
• Local encrypted memory
• Web search and page summary (with your permission)
• Generate project skeletons and code
• Prepare app / payment / upload flows with confirmation

Say what you want in plain language."""
    }
}
