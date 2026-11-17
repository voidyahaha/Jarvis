package com.aether.assistant.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aether.assistant.AppContainer
import com.aether.assistant.ui.theme.Cyan
import com.aether.assistant.ui.theme.Panel

@Composable
fun AppsScreen(container: AppContainer) {
    val context = LocalContext.current
    val pm = context.packageManager
    val apps = remember {
        pm.getInstalledApplications(0)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .sortedBy { pm.getApplicationLabel(it).toString() }
            .take(120)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("APP VAULT", style = MaterialTheme.typography.headlineMedium, color = Cyan)
        Text("Floating app cards. Tap to launch through Android Intents.",
            style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(10.dp))
        LazyColumn {
            items(apps) { app ->
                val label = pm.getApplicationLabel(app).toString()
                Row(Modifier.padding(vertical = 4.dp)) {
                    Text("▣ $label", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { container.automation.openApp(label) },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Panel, contentColor = Cyan)) {
                        Text("Open")
                    }
                }
            }
        }
    }
}
