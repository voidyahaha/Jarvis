package com.aether.assistant

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aether.assistant.core.settings.AiMode
import com.aether.assistant.ui.components.ConfirmationDialog
import com.aether.assistant.ui.screens.AppsScreen
import com.aether.assistant.ui.screens.ChatScreen
import com.aether.assistant.ui.screens.CodeLabScreen
import com.aether.assistant.ui.screens.FilesScreen
import com.aether.assistant.ui.screens.HomeScreen
import com.aether.assistant.ui.screens.MemoryScreen
import com.aether.assistant.ui.screens.PrivacyScreen
import com.aether.assistant.ui.screens.SettingsScreen
import com.aether.assistant.ui.screens.VisionScreen
import com.aether.assistant.ui.theme.AetherTheme
import com.aether.assistant.ui.theme.Cyan
import com.aether.assistant.ui.theme.Panel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AetherTheme {
                AetherApp()
            }
        }
    }
}

private val DESTINATIONS = listOf(
    "home" to "🧠 Core",
    "vision" to "👁️ Vision",
    "files" to "📁 Files",
    "code" to "💻 Code Lab",
    "apps" to "📱 Apps",
    "memory" to "🧠 Memory",
    "privacy" to "🔐 Privacy",
    "settings" to "⚙️ Settings"
)

@Composable
private fun AetherApp() {
    val context = LocalContext.current
    val app = context.applicationContext as AetherApplication
    val c = app.container
    val nav = rememberNavController()

    val settings by c.settings.settings.collectAsState(initial = com.aether.assistant.core.settings.AppSettings())
    val agentState by c.agent.state.collectAsState()
    val confirmation by c.agent.confirmation.collectAsState()
    val voiceState by c.voice.state.collectAsState()

    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(settings.mode, settings.endpoint) {
        c.aiManager.configure(settings.mode, settings.endpoint, c.localEngine)
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Scaffold(
        containerColor = com.aether.assistant.ui.theme.Void,
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("AETHER", style = MaterialTheme.typography.displayLarge, color = Cyan)
                Spacer(Modifier.weight(1f))
                com.aether.assistant.ui.components.AiStatusChip(
                    label = c.aiManager.statusLabel(c.localEngine),
                    online = settings.mode == AiMode.ONLINE
                )
            }
        },
        bottomBar = { HudNavBar(nav) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(nav, startDestination = "home") {
                composable("home") {
                    HomeScreen(
                        container = c,
                        settings = settings,
                        agentState = agentState,
                        voiceState = voiceState,
                        selectedVideoUri = selectedVideoUri,
                        onVideoPicked = { selectedVideoUri = it },
                        onRequestMic = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                                micPermission.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onSubmit = { text, extras -> c.agent.submit(text, extras) },
                        onOpenChat = { nav.navigate("chat") }
                    )
                }
                composable("chat") { ChatScreen(c) }
                composable("vision") {
                    VisionScreen(
                        container = c,
                        onRequestCamera = {
                            cameraPermission.launch(Manifest.permission.CAMERA)
                        },
                        onQrDetected = { raw -> c.agent.submit("Read QR: $raw") }
                    )
                }
                composable("files") {
                    FilesScreen(
                        container = c,
                        selectedUri = selectedFileUri,
                        onUriSelected = { selectedFileUri = it }
                    )
                }
                composable("code") { CodeLabScreen(c) }
                composable("apps") { AppsScreen(c) }
                composable("memory") { MemoryScreen(c) }
                composable("privacy") { PrivacyScreen(c) }
                composable("settings") { SettingsScreen(c, settings) }
            }
        }
        ConfirmationDialog(
            request = confirmation,
            onApprove = { c.agent.approve() },
            onDeny = { c.agent.deny() }
        )
    }
}

@Composable
private fun HudNavBar(nav: NavHostController) {
    val current by nav.currentBackStackEntryAsState()
    val route = current?.destination?.route
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .background(Panel.copy(alpha = 0.9f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        DESTINATIONS.forEach { (dest, label) ->
            val active = route == dest
            Text(
                label,
                color = if (active) Cyan else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (active) Cyan.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { nav.navigate(dest) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                maxLines = 1
            )
            Spacer(Modifier.height(0.dp))
        }
    }
}
