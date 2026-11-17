package com.aether.assistant.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aether.assistant.AppContainer
import com.aether.assistant.core.settings.AiEndpoint
import com.aether.assistant.core.settings.AiMode
import com.aether.assistant.core.settings.AppSettings
import com.aether.assistant.ui.theme.Cyan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(container: AppContainer, settings: AppSettings) {
    var url by remember(settings.endpoint.baseUrl) { mutableStateOf(settings.endpoint.baseUrl) }
    var model by remember(settings.endpoint.model) { mutableStateOf(settings.endpoint.model) }
    var token by remember(settings.endpoint.token) { mutableStateOf(settings.endpoint.token) }
    var youtubeToken by remember { mutableStateOf(container.youtubeToken) }

    fun saveEndpoint() {
        CoroutineScope(Dispatchers.Default).launch {
            container.settings.setEndpoint(
                AiEndpoint("user-endpoint", url.trim(), model.trim(), token.trim())
            )
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("SETTINGS", style = MaterialTheme.typography.headlineMedium, color = Cyan)

        Spacer(Modifier.height(14.dp))
        Text("AI MODE", style = MaterialTheme.typography.labelSmall, color = Cyan)
        Row {
            Button(onClick = { CoroutineScope(Dispatchers.Default).launch { container.settings.setMode(AiMode.OFFLINE) } },
                enabled = settings.mode != AiMode.OFFLINE) { Text("🟢 Offline") }
            Spacer(Modifier.padding(horizontal = 6.dp))
            Button(onClick = { CoroutineScope(Dispatchers.Default).launch { container.settings.setMode(AiMode.ONLINE) } },
                enabled = settings.mode != AiMode.ONLINE) { Text("🔵 Online") }
        }
        Text("Current: ${settings.mode}. Online is opt-in and never automatic.",
            style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(16.dp))
        Text("AI ENDPOINT (OpenAI-compatible)", style = MaterialTheme.typography.labelSmall, color = Cyan)
        Text("Use 127.0.0.1:8080 for a local llama.cpp / Ollama server, or any free/user-provided endpoint.",
            style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Base URL") },
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model name") },
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = token, onValueChange = { token = it }, label = { Text("API token (stored encrypted)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button(onClick = { saveEndpoint() }) { Text("Save endpoint") }

        Spacer(Modifier.height(16.dp))
        Text("YOUTUBE OAUTH TOKEN", style = MaterialTheme.typography.labelSmall, color = Cyan)
        Text("Paste a user-provided access token. It is never bundled or sent anywhere except Google's API when you upload.",
            style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(value = youtubeToken, onValueChange = {
            youtubeToken = it
            container.youtubeToken = it.trim()
        },
            label = { Text("Access token (session)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text("Uploads read the token from this field when you approve an upload.",
            style = MaterialTheme.typography.labelSmall)

        Spacer(Modifier.height(16.dp))
        Text("CAPABILITIES", style = MaterialTheme.typography.labelSmall, color = Cyan)
        ToggleRow("Hands-free conversation", settings.handsFree) {
            CoroutineScope(Dispatchers.Default).launch { container.settings.setHandsFree(it) }
        }
        ToggleRow("Screen intelligence", settings.screenIntelligence) {
            CoroutineScope(Dispatchers.Default).launch { container.settings.setScreen(it) }
        }
        ToggleRow("Notification intelligence", settings.notificationIntelligence) {
            CoroutineScope(Dispatchers.Default).launch { container.settings.setNotifications(it) }
        }
        ToggleRow("Allow online vision", settings.onlineVision) {
            CoroutineScope(Dispatchers.Default).launch { container.settings.setOnlineVision(it) }
        }
        ToggleRow("Allow online file analysis", settings.onlineFiles) {
            CoroutineScope(Dispatchers.Default).launch { container.settings.setOnlineFiles(it) }
        }
        ToggleRow("Confirm sensitive actions", settings.confirmSensitive) {
            CoroutineScope(Dispatchers.Default).launch { container.settings.setConfirm(it) }
        }
        ToggleRow("Speak replies", settings.autoTts) {
            CoroutineScope(Dispatchers.Default).launch { container.settings.setAutoTts(it) }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
