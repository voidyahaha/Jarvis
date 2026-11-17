package com.aether.assistant.core.agent

import android.net.Uri
import com.aether.assistant.core.util.containsAny

/** Planner → converts a user goal into verifiable steps. */
class Planner {

    fun plan(input: String, extras: Map<String, Any>): TaskPlan {
        val steps = mutableListOf<TaskStep>()
        val lower = input.lowercase()

        fun add(action: AgentAction, label: String) {
            steps += TaskStep(action, label)
        }

        when {
            lower.containsAny("phonepe", "pay", "upi", "scanner") -> {
                add(
                    AgentAction.Sensitive(
                        "Open PhonePe's scanner and read the QR? You will confirm any payment yourself.",
                        AgentAction.OpenPhonePeScanner
                    ),
                    "Open payment scanner (confirmation required)"
                )
            }

            lower.containsAny("youtube", "upload", "yt ") -> {
                val videoUri = extras["videoUri"] as? Uri
                if (videoUri == null) {
                    add(AgentAction.Chat("To upload, pick a video first using the 📁 Files screen or Video chip."), "Ask for video")
                } else {
                    val title = extractAfter(input, "title", "called", "named") ?: "Aether upload"
                    val description = extractAfter(input, "description", "desc") ?: "Uploaded by Aether Assistant"
                    val schedule = extras["scheduleMillis"] as? Long
                    add(
                        AgentAction.Sensitive(
                            "Upload${if (schedule != null) " and schedule" else ""} this video to YouTube?",
                            AgentAction.YouTubeUpload(videoUri, title, description, schedule)
                        ),
                        "Upload to YouTube"
                    )
                }
            }

            lower.containsAny("instagram", "insta", "follow") -> {
                val target = extractAfter(input, "search", "find", "for") ?: input.removePrefix("search").trim()
                add(AgentAction.OpenApp("instagram"), "Open Instagram")
                add(AgentAction.Click("search"), "Tap search")
                add(AgentAction.TypeText(target), "Type \"$target\"")
                add(
                    AgentAction.Sensitive("Follow \"$target\" on Instagram?", AgentAction.Click("Follow")),
                    "Tap Follow (confirmation required)"
                )
            }

            lower.containsAny("build", "create", "generate", "make") &&
                lower.containsAny("app", "site", "website", "project", "code", "source") -> {
                add(AgentAction.GenerateProject(input), "Generate project source")
            }

            lower.containsAny("remember", "note this", "save this") -> {
                val content = input.substringAfter(":", input).removePrefix("remember").trim()
                add(
                    AgentAction.Remember(
                        title = content.take(40).ifBlank { "Memory" },
                        content = content,
                        sensitive = lower.containsAny("password", "secret", "pin", "private")
                    ),
                    "Store local memory"
                )
            }

            lower.containsAny("search web", "google", "lookup", "find online") -> {
                add(AgentAction.WebSearch(input.replace(Regex("(?i)(search web|google|lookup|find online)"), "").trim()), "Search the web")
            }

            lower.containsAny("http://", "https://", "read page", "summarize page") -> {
                val url = Regex("https?://\\S+").find(input)?.value.orEmpty()
                if (url.isNotBlank()) add(AgentAction.ReadPage(url), "Read webpage")
                else add(AgentAction.Chat("Send me a link to read."), "Ask for link")
            }

            lower.containsAny("open", "launch") -> {
                val query = input.replace(Regex("(?i)^(open|launch|start)\\s+"), "").trim()
                add(AgentAction.OpenApp(query), "Open $query")
            }

            lower.containsAny("camera", "scan", "look at") -> {
                add(AgentAction.CameraVision(includeImageInAi = false), "Analyze camera view")
            }

            lower.containsAny("screen", "what's on", "what is on") -> {
                add(AgentAction.ScreenText, "Read screen")
            }

            else -> add(AgentAction.Chat(input), "Answer with ${if (lower.containsAny("online")) "online" else "active"} AI")
        }

        return TaskPlan(input, steps)
    }

    private fun extractAfter(input: String, vararg keys: String): String? {
        for (key in keys) {
            val rx = Regex("$key\\s+(?:is\\s+)?[\"']?([^\"',]+)[\"']?", Regex.IGNORE_CASE)
            rx.find(input)?.groupValues?.get(1)?.trim()?.let { if (it.isNotBlank()) return it }
        }
        return null
    }
}
