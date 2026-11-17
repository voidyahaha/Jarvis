package com.aether.assistant.core.agent

import android.net.Uri

sealed class AgentAction(val id: String) {
    data class Chat(val text: String, val images: List<String> = emptyList()) : AgentAction("chat")
    data class OpenApp(val query: String) : AgentAction("open_app")
    data class WebSearch(val query: String) : AgentAction("web_search")
    data class ReadPage(val url: String) : AgentAction("read_page")
    data class Click(val target: String) : AgentAction("click")
    data class TypeText(val text: String) : AgentAction("type")
    object Scroll : AgentAction("scroll")
    object ScreenText : AgentAction("screen_text")
    object OpenPhonePeScanner : AgentAction("phonepe_scan")
    data class AnalyzeFile(val uri: Uri) : AgentAction("analyze_file")
    data class Remember(val title: String, val content: String, val sensitive: Boolean) : AgentAction("remember")
    data class GenerateProject(val prompt: String) : AgentAction("generate_project")
    data class CameraVision(val includeImageInAi: Boolean) : AgentAction("camera_vision")
    data class YouTubeUpload(
        val uri: Uri,
        val title: String,
        val description: String,
        val publishAtMillis: Long?
    ) : AgentAction("youtube_upload")
    data class Sensitive(val reason: String, val inner: AgentAction) : AgentAction("confirm")
    data class Unknown(val text: String) : AgentAction("unknown")
}

enum class StepStatus { PENDING, RUNNING, DONE, FAILED, SKIPPED }

data class TaskStep(
    val action: AgentAction,
    val label: String,
    val status: StepStatus = StepStatus.PENDING,
    val detail: String = ""
)

data class TaskPlan(val goal: String, val steps: List<TaskStep>)

data class ConfirmationRequest(
    val reason: String,
    val onApprove: () -> Unit,
    val onDeny: () -> Unit
)

data class AgentState(
    val plan: TaskPlan? = null,
    val messages: List<String> = emptyList(),
    val running: Boolean = false,
    val lastError: String? = null
)
