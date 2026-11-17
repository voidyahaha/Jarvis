package com.aether.assistant.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aether.assistant.AppContainer
import com.aether.assistant.core.agent.AgentAction
import com.aether.assistant.core.ai.ChatMessage
import com.aether.assistant.core.ai.Role
import com.aether.assistant.ui.theme.Cyan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(container: AppContainer) {
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf<ChatMessage>() }

    fun send() {
        val text = input.trim()
        if (text.isBlank() || busy) return
        messages += ChatMessage(Role.USER, text)
        input = ""
        busy = true
        CoroutineScope(Dispatchers.Default).launch {
            val result = container.executor.execute(AgentAction.Chat(text), emptyMap())
            val reply = result.getOrElse { "Error: ${it.message}" }
            launch(Dispatchers.Main) {
                messages += ChatMessage(Role.ASSISTANT, reply)
                container.voice.speak(reply)
                busy = false
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("CONVERSATION", style = MaterialTheme.typography.headlineMedium, color = Cyan)
        Spacer(Modifier.height(10.dp))
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            items(messages) { m ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    val prefix = if (m.role == Role.USER) "🧑" else "🤖"
                    Text("$prefix ", color = if (m.role == Role.USER) Cyan else MaterialTheme.colorScheme.primary)
                    Text(m.content, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message Aether…") },
                maxLines = 3
            )
            Spacer(Modifier.padding(horizontal = 6.dp))
            Button(onClick = { send() }, enabled = !busy) { Text(if (busy) "…" else "Send") }
        }
    }
}
