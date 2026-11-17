package com.aether.assistant.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.aether.assistant.AppContainer
import com.aether.assistant.core.agent.AgentState
import com.aether.assistant.core.settings.AppSettings
import com.aether.assistant.core.voice.VoiceState
import com.aether.assistant.ui.components.TaskProgressOverlay
import com.aether.assistant.ui.components.VoiceVisualizer
import com.aether.assistant.ui.theme.Cyan
import com.aether.assistant.ui.theme.Panel
import com.aether.assistant.ui.theme.Red
import com.aether.assistant.ui.theme.TextMuted

@Composable
fun HomeScreen(
    container: AppContainer,
    settings: AppSettings,
    agentState: AgentState,
    voiceState: VoiceState,
    selectedVideoUri: android.net.Uri?,
    onVideoPicked: (android.net.Uri?) -> Unit,
    onRequestMic: () -> Unit,
    onSubmit: (String, Map<String, Any>) -> Unit,
    onOpenChat: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var scheduleMinutes by remember { mutableStateOf("") }
    var lastSpokenCount by remember { mutableStateOf(0) }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        onVideoPicked(uri)
    }

    fun send(text: String) {
        if (text.isBlank()) return
        val extras = mutableMapOf<String, Any>()
        extras["youtubeToken"] = container.youtubeToken
        selectedVideoUri?.let { extras["videoUri"] = it }
        scheduleMinutes.toLongOrNull()?.let { extras["scheduleMillis"] = System.currentTimeMillis() + it * 60_000 }
        onSubmit(text, extras)
        input = ""
    }

    LaunchedEffect(agentState.running, agentState.messages.size, settings.autoTts, settings.handsFree) {
        if (!agentState.running && agentState.messages.size > lastSpokenCount && settings.autoTts) {
            lastSpokenCount = agentState.messages.size
            container.voice.speak(agentState.messages.last())
            if (settings.handsFree) {
                container.voice.startListening { heard -> send(heard) }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) {
            VoiceVisualizer(
                modifier = Modifier.size(220.dp),
                active = voiceState.listening,
                speaking = voiceState.speaking,
                color = Cyan
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    when {
                        voiceState.listening -> "LISTENING…"
                        voiceState.speaking -> "SPEAKING"
                        agentState.running -> "EXECUTING"
                        else -> "AETHER READY"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Cyan
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (voiceState.partial.isNotBlank()) voiceState.partial else "How can I help?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        voiceState.lastError?.let { Text("⚠ $it", color = Red, style = MaterialTheme.typography.labelSmall) }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    onRequestMic()
                    container.voice.startListening { heard ->
                        input = heard
                        send(heard)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Cyan.copy(alpha = 0.15f), contentColor = Cyan)
            ) { Text(if (voiceState.listening) "⏹ Stop" else "🎙 Speak") }

            Spacer(Modifier.padding(horizontal = 6.dp))

            Button(
                onClick = { container.voice.interrupt() },
                colors = ButtonDefaults.buttonColors(containerColor = Panel, contentColor = Red)
            ) { Text("✋ Interrupt") }

            Spacer(Modifier.padding(horizontal = 6.dp))

            Button(
                onClick = onOpenChat,
                colors = ButtonDefaults.buttonColors(containerColor = Panel, contentColor = Cyan)
            ) { Text("💬 Chat") }
        }

        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Tell Aether what to do…", color = TextMuted) },
            singleLine = false,
            maxLines = 3,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(Modifier.height(8.dp))
        Row {
            Button(onClick = { send(input) }, enabled = input.isNotBlank()) { Text("▶ Execute") }
            Spacer(Modifier.padding(horizontal = 6.dp))
            Button(onClick = { videoPicker.launch("video/*") }, colors = ButtonDefaults.buttonColors(containerColor = Panel)) {
                Text(if (selectedVideoUri != null) "🎬 Video ready" else "🎬 Pick video")
            }
        }

        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = scheduleMinutes,
            onValueChange = { scheduleMinutes = it.filter { ch -> ch.isDigit() } },
            label = { Text("Schedule upload in N minutes (optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))
        Text("QUICK COMMANDS", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Spacer(Modifier.height(6.dp))
        val chips = listOf(
            "Open camera" to "open camera",
            "Search web for android AI" to "search web for android AI",
            "Build a website called Aurora" to "build a website called Aurora",
            "Remember: my Wi-Fi password is hidden" to "remember: my Wi-Fi password is hidden",
            "Read my screen" to "read my screen",
            "Open PhonePe scanner" to "open phonepe and pay via scanner",
            "Upload video to YouTube titled Demo" to "upload video on yt titled Demo description Aether test"
        )
        Column(Modifier.fillMaxWidth()) {
            chips.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth()) {
                    pair.forEach { (label, cmd) ->
                        Text(
                            label,
                            color = Cyan,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { send(cmd) }
                                .padding(10.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        TaskProgressOverlay(agentState)

        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth()) {
            agentState.messages.takeLast(12).forEach { msg ->
                Text(
                    msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
