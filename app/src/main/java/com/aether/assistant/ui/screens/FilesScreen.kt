package com.aether.assistant.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aether.assistant.AppContainer
import com.aether.assistant.core.files.FileAnalysis
import com.aether.assistant.ui.theme.Cyan
import com.aether.assistant.ui.theme.Red
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun FilesScreen(
    container: AppContainer,
    selectedUri: android.net.Uri?,
    onUriSelected: (android.net.Uri?) -> Unit
) {
    var progress by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<FileAnalysis?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        onUriSelected(uri)
        if (uri != null) {
            result = null
            error = null
            CoroutineScope(Dispatchers.Default).launch {
                val r = container.files.analyze(uri) { p ->
                    launch(Dispatchers.Main) { progress = p }
                }
                launch(Dispatchers.Main) {
                    progress = ""
                    r.onSuccess { result = it }.onFailure { error = it.message }
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("FILE ANALYSIS", style = MaterialTheme.typography.headlineMedium, color = Cyan)
        Text(
            "PDF · Images · Audio · Video · ZIP · Spreadsheets · Code",
            style = MaterialTheme.typography.labelSmall,
            color = com.aether.assistant.ui.theme.TextMuted
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = { picker.launch("*/*") }) { Text("📁 Choose file from storage") }

        if (progress.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator(color = Cyan)
            Spacer(Modifier.height(8.dp))
            Text(progress, style = MaterialTheme.typography.bodyMedium)
        }

        error?.let { Text("⚠ $it", color = Red, style = MaterialTheme.typography.bodyMedium) }

        result?.let { r ->
            Spacer(Modifier.height(18.dp))
            Text(r.name, style = MaterialTheme.typography.headlineMedium, color = Cyan)
            Text("${r.mime} · ${r.sizeBytes / 1024} KB", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(8.dp))
            Text(r.summary, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Column(Modifier.fillMaxWidth()) {
                r.details.forEach { d ->
                    Text("• $d", style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}
