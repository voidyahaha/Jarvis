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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aether.assistant.AppContainer
import com.aether.assistant.core.code.GeneratedFile
import com.aether.assistant.core.code.GeneratedProject
import com.aether.assistant.ui.theme.Cyan
import com.aether.assistant.ui.theme.Panel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun CodeLabScreen(container: AppContainer) {
    val context = LocalContext.current
    var prompt by remember { mutableStateOf("Build a website called Aurora") }
    var project by remember { mutableStateOf<GeneratedProject?>(null) }
    var openFile by remember { mutableStateOf<GeneratedFile?>(null) }

    fun generate() {
        CoroutineScope(Dispatchers.Default).launch {
            val p = container.code.generate(prompt)
            container.code.writeTo(context.applicationContext.filesDir, p)
            launch(Dispatchers.Main) {
                project = p
                openFile = p.files.firstOrNull()
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("CODE LAB", style = MaterialTheme.typography.headlineMedium, color = Cyan)
        Text("Generate original source-code projects locally from plain language.",
            style = MaterialTheme.typography.labelSmall)

        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = prompt, onValueChange = { prompt = it },
            label = { Text("What should I build?") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
        Button(onClick = { generate() }) { Text("⚙ Generate project") }

        project?.let { p ->
            Spacer(Modifier.height(12.dp))
            Text("${p.name} · ${p.kind}", style = MaterialTheme.typography.headlineMedium, color = Cyan)
            LazyColumn(Modifier.weight(1f)) {
                items(p.files) { f ->
                    Text(
                        "📄 ${f.path}",
                        color = if (openFile == f) Cyan else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { openFile = f }
                            .padding(10.dp)
                    )
                }
            }
        }

        openFile?.let { f ->
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(10.dp)
            ) {
                Text(f.path, style = MaterialTheme.typography.labelSmall, color = Cyan)
                Text(f.content, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
