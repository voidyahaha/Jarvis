package com.aether.assistant.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.aether.assistant.AppContainer
import com.aether.assistant.ui.theme.Cyan
import com.aether.assistant.ui.theme.Red
import com.aether.assistant.ui.theme.TextMuted

@Composable
fun VisionScreen(
    container: AppContainer,
    onRequestCamera: () -> Unit,
    onQrDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val insight by container.camera.insight.collectAsState()
    val active by container.camera.active.collectAsState()

    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    DisposableEffect(Unit) {
        onDispose { container.camera.close() }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("CAMERA VISION", style = MaterialTheme.typography.headlineMedium, color = Cyan)
        Text(
            if (active) "● CAMERA ACTIVE" else "○ CAMERA OFF",
            color = if (active) Red else TextMuted,
            style = MaterialTheme.typography.labelSmall
        )
        Spacer(Modifier.height(10.dp))

        if (!granted) {
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA); onRequestCamera() }) {
                Text("Grant camera permission")
            }
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(androidx.compose.ui.graphics.Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PreviewView(ctx).also { pv ->
                            container.camera.bind(pv, lifecycle) { /* permission toast inside analyzer */ }
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            container.camera.captureBase64 { b64 ->
                if (b64 != null) {
                    container.agent.submit("Analyze this camera image", mapOf("cameraImage" to b64))
                }
            }
        }) { Text("📸 Capture & analyze") }

        Spacer(Modifier.height(14.dp))
        Text("ON-DEVICE INSIGHT", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Spacer(Modifier.height(6.dp))
        Text("TEXT", style = MaterialTheme.typography.labelSmall, color = Cyan)
        Text(insight.text.take(1200).ifBlank { "(no text detected)" }, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Text("BARCodes / QR", style = MaterialTheme.typography.labelSmall, color = Cyan)
        insight.barcodes.forEach { raw ->
            Text(raw, style = MaterialTheme.typography.bodyMedium)
            if (raw.startsWith("upi://", true)) {
                Button(onClick = { onQrDetected(raw) }) { Text("Prepare payment (confirmation required)") }
            }
        }
        if (insight.barcodes.isEmpty()) Text("(none)", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Text("LABELS", style = MaterialTheme.typography.labelSmall, color = Cyan)
        Text(insight.labels.joinToString(", ").ifBlank { "(none)" }, style = MaterialTheme.typography.bodyMedium)
    }
}
