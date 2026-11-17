package com.aether.assistant.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aether.assistant.AppContainer
import com.aether.assistant.core.util.AppTime
import com.aether.assistant.ui.theme.Cyan
import com.aether.assistant.ui.theme.Red
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun MemoryScreen(container: AppContainer) {
    val memories by container.memory.observeAll().collectAsState(initial = emptyList())
    var query by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var sensitive by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<com.aether.assistant.core.memory.MemoryEntity>?>(null) }
    val shown = results ?: memories

    fun refreshSearch() {
        if (query.isBlank()) { results = null; return }
        CoroutineScope(Dispatchers.Default).launch {
            val r = container.memory.search(query)
            launch(Dispatchers.Main) { results = r }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("LOCAL MEMORY", style = MaterialTheme.typography.headlineMedium, color = Cyan)
        Text("Encrypted at rest with Android Keystore when marked sensitive.",
            style = MaterialTheme.typography.labelSmall)

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query, onValueChange = { query = it; refreshSearch() },
            label = { Text("Search memories") }, modifier = Modifier.fillMaxWidth(), singleLine = true
        )

        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("What to remember") },
            modifier = Modifier.fillMaxWidth(), maxLines = 3)
        Row {
            Button(onClick = { sensitive = !sensitive }) {
                Text(if (sensitive) "🔒 Encrypted" else "🔓 Plain local")
            }
            Spacer(Modifier.padding(horizontal = 6.dp))
            Button(onClick = {
                if (title.isNotBlank() && content.isNotBlank()) {
                    CoroutineScope(Dispatchers.Default).launch {
                        container.memory.remember(title, content, sensitive, "ui")
                        launch(Dispatchers.Main) { title = ""; content = "" }
                    }
                }
            }) { Text("Save") }
        }

        Spacer(Modifier.height(10.dp))
        Button(onClick = {
            CoroutineScope(Dispatchers.Default).launch { container.memory.forgetAll() }
        }, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Red.copy(alpha = 0.2f), contentColor = Red)) {
            Text("🗑 Delete all memory")
        }

        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.fillMaxSize()) {
            items(shown) { m ->
                var revealed by remember { mutableStateOf(false) }
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(m.title, color = Cyan, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (m.sensitive && !revealed) "🔒 Encrypted — tap to reveal"
                            else if (m.sensitive) runCatching {
                                kotlinx.coroutines.runBlocking { container.memory.read(m) }
                            }.getOrDefault("(unable to decrypt)") else m.content,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.clickable { revealed = !revealed }
                        )
                        Text(AppTime.readable(m.createdAt), style = MaterialTheme.typography.labelSmall)
                    }
                    Button(onClick = {
                        CoroutineScope(Dispatchers.Default).launch { container.memory.forget(m) }
                    }) { Text("✖") }
                }
            }
        }
    }
}
