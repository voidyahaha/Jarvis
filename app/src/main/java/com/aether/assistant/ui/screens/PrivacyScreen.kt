package com.aether.assistant.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aether.assistant.AppContainer
import com.aether.assistant.core.permissions.PermissionState
import com.aether.assistant.ui.theme.Cyan
import com.aether.assistant.ui.theme.Green
import com.aether.assistant.ui.theme.Red
import com.aether.assistant.ui.theme.TextMuted
import com.aether.assistant.ui.theme.Amber

@Composable
fun PrivacyScreen(container: AppContainer) {
    val context = LocalContext.current
    val settings by container.settings.settings.collectAsState(initial = com.aether.assistant.core.settings.AppSettings())
    val perms = remember { container.permissions.snapshot() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("PRIVACY & SECURITY", style = MaterialTheme.typography.headlineMedium, color = Cyan)
        Text("Local-first. No telemetry, no ads, no tracking SDKs, no hidden uploads.",
            style = MaterialTheme.typography.labelSmall, color = TextMuted)

        Spacer(Modifier.height(16.dp))
        Text("PERMISSION DASHBOARD", style = MaterialTheme.typography.labelSmall, color = Cyan)
        perms.forEach { p ->
            Spacer(Modifier.height(8.dp))
            Row {
                val color = when (p.state) {
                    PermissionState.GRANTED -> Green
                    PermissionState.DENIED -> Red
                    PermissionState.NOT_GRANTED -> Amber
                }
                Text("●", color = color, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Column {
                    Text(p.label, color = color, style = MaterialTheme.typography.bodyLarge)
                    Text(p.state.name, style = MaterialTheme.typography.labelSmall, color = color)
                    Text(p.explanation, style = MaterialTheme.typography.bodyMedium)
                    p.settingsAction?.let { action ->
                        androidx.compose.material3.TextButton(onClick = {
                            runCatching { context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        }) { Text("Open system settings") }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("DATA CONTROLS", style = MaterialTheme.typography.labelSmall, color = Cyan)
        Spacer(Modifier.height(6.dp))
        Text("AI mode: ${settings.mode}", style = MaterialTheme.typography.bodyMedium)
        Text("Online AI: ${if (settings.mode == com.aether.assistant.core.settings.AiMode.ONLINE) "enabled by you" else "fully disabled"}",
            style = MaterialTheme.typography.bodyMedium)
        Text("Notification intelligence: ${settings.notificationIntelligence}", style = MaterialTheme.typography.bodyMedium)
        Text("Screen intelligence: ${settings.screenIntelligence}", style = MaterialTheme.typography.bodyMedium)
        Text("Sensitive-action confirmation: ${settings.confirmSensitive}", style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(14.dp))
        Text(
            "Nothing in this app requests root, exploits vulnerabilities, or bypasses Android permissions. " +
                "Camera/mic/screen features show visible indicators and require your action.",
            style = MaterialTheme.typography.bodyMedium, color = TextMuted
        )
    }
}
