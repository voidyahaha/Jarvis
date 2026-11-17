package com.aether.assistant.core.agent

import android.content.Context
import com.aether.assistant.core.ai.AiManager
import com.aether.assistant.core.ai.AiRequest
import com.aether.assistant.core.ai.ChatMessage
import com.aether.assistant.core.ai.LocalEngine
import com.aether.assistant.core.ai.Role
import com.aether.assistant.core.automation.AutomationController
import com.aether.assistant.core.automation.PaymentFlow
import com.aether.assistant.core.code.CodeGenerator
import com.aether.assistant.core.files.FileAnalyzer
import com.aether.assistant.core.memory.MemoryRepository
import com.aether.assistant.core.settings.SettingsRepository
import com.aether.assistant.core.vision.CameraAnalyzer
import com.aether.assistant.core.web.WebRepository
import com.aether.assistant.core.youtube.YouTubeUploader
import kotlinx.coroutines.flow.first

/** Executor → performs each verified step and returns an observable result. */
class Executor(
    private val context: Context,
    private val aiManager: AiManager,
    private val localEngine: LocalEngine,
    private val settings: SettingsRepository,
    private val memory: MemoryRepository,
    private val automation: AutomationController,
    private val payment: PaymentFlow,
    private val web: WebRepository,
    private val files: FileAnalyzer,
    private val code: CodeGenerator,
    private val camera: CameraAnalyzer,
    private val youtube: YouTubeUploader
) {

    suspend fun execute(action: AgentAction, extras: Map<String, Any>): Result<String> = when (action) {
        is AgentAction.Chat -> chat(action, extras)
        is AgentAction.OpenApp -> automation.openApp(action.query)
        is AgentAction.WebSearch -> web.search(action.query).map { results ->
            if (results.isEmpty()) "No web results."
            else results.joinToString("\n\n") { "• ${it.title}\n  ${it.url}\n  ${it.snippet.take(180)}" }
        }
        is AgentAction.ReadPage -> web.readPage(action.url).map { "Summary source:\n${it.take(2500)}" }
        is AgentAction.Click -> automation.click(action.target)
        is AgentAction.TypeText -> automation.type(action.text)
        AgentAction.Scroll -> automation.scroll()
        AgentAction.ScreenText -> automation.screenText().map { "Screen contents:\n$it" }
        AgentAction.OpenPhonePeScanner -> openScanner()
        is AgentAction.AnalyzeFile -> files.analyze(action.uri) { /* progress shown by UI via state */ }
            .map { result ->
                buildString {
                    append("### ${result.name}\n${result.summary}\n")
                    result.details.take(20).forEach { append("• $it\n") }
                }
            }
        is AgentAction.Remember -> runCatching {
            memory.remember(action.title, action.content, action.sensitive, "agent")
            "Stored locally in ${if (action.sensitive) "encrypted" else "local"} memory."
        }
        is AgentAction.GenerateProject -> runCatching {
            val project = code.generate(action.prompt)
            val root = code.writeTo(context.filesDir, project)
            "Generated ${project.kind} project at ${root.absolutePath}\n" +
                project.files.joinToString("\n") { "📄 ${it.path}" }
        }
        is AgentAction.CameraVision -> runCatching {
            val insight = camera.insight.value
            buildString {
                append("Camera insight (on-device):\n")
                append("Text: ${insight.text.take(800).ifBlank { "(none)" }}\n")
                append("Barcodes/QR: ${insight.barcodes.joinToString(", ").ifBlank { "(none)" }}\n")
                append("Labels: ${insight.labels.joinToString(", ").ifBlank { "(none)" }}\n")
                insight.barcodes.firstOrNull { it.startsWith("upi://", true) }?.let { raw ->
                    payment.parseUpi(raw)?.let { info ->
                        append("\nUPI QR detected — payee: ${info.payee}, UPI: ${info.upiId}, amount: ${info.amount ?: "not set"}.\n")
                        append("Aether has NOT paid. Confirm and enter your UPI PIN inside PhonePe.")
                    }
                }
            }
        }
        is AgentAction.YouTubeUpload -> runCatching {
            val token = extras["youtubeToken"] as? String ?: ""
            val result = youtube.upload(
                YouTubeUploader.UploadRequest(
                    videoFile = java.io.File(getPath(action.uri) ?: error("Cannot resolve video path")),
                    title = action.title,
                    description = action.description,
                    publishAtMillis = action.publishAtMillis
                ),
                token
            )
            result.getOrThrow()
        }
        is AgentAction.Sensitive -> error("Sensitive actions must be confirmed by the agent")
        is AgentAction.Unknown -> Result.success("I didn't understand that. Try 'open camera', 'search web for…', or 'build a website called…'.")
    }

    private suspend fun chat(action: AgentAction.Chat, extras: Map<String, Any>): Result<String> = runCatching {
        val s = settings.settings.first()
        val recalled = runCatching { memory.search(action.text.take(60)).take(3) }.getOrDefault(emptyList())
        val contextBlock = if (recalled.isEmpty()) "" else
            "\n\nRelevant local memories:\n" + recalled.joinToString("\n") { "- ${it.title}: ${it.content.take(160)}" }

        val images = action.images + listOfNotNull(extras["cameraImage"] as? String)
        aiManager.active(localEngine).generate(
            AiRequest(
                messages = listOf(
                    ChatMessage(Role.SYSTEM, "You are Aether, a local-first Android assistant. Be concise and honest about limitations."),
                    ChatMessage(Role.USER, action.text + contextBlock)
                ),
                base64Images = images
            )
        ).getOrThrow().text
    }

    private fun openScanner(): Result<String> = runCatching {
        val opened = payment.openScanner().getOrThrow()
        val qr = camera.insight.value.barcodes.firstOrNull { it.startsWith("upi://", true) }
        if (qr == null) opened else {
            val info = payment.parseUpi(qr)
            "$opened\nDetected UPI QR: ${info?.payee} (${info?.upiId}) amount ${info?.amount ?: "not set"}.\n" +
                "No payment was made. Confirm inside PhonePe with your own PIN."
        }
    }

    private fun getPath(uri: android.net.Uri): String? {
        val cr = context.contentResolver
        return cr.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        }
    }
}
