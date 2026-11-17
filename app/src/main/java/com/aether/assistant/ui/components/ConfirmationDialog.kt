package com.aether.assistant.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.aether.assistant.core.agent.ConfirmationRequest
import com.aether.assistant.ui.theme.Amber

@Composable
fun ConfirmationDialog(request: ConfirmationRequest?, onApprove: () -> Unit, onDeny: () -> Unit) {
    if (request == null) return
    AlertDialog(
        onDismissRequest = onDeny,
        title = { Text("⚠ Confirm action", color = Amber, style = MaterialTheme.typography.headlineMedium) },
        text = { Text(request.reason, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { TextButton(onClick = onApprove) { Text("Approve") } },
        dismissButton = { TextButton(onClick = onDeny) { Text("Cancel") } }
    )
}
