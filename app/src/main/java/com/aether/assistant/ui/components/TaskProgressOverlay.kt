package com.aether.assistant.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aether.assistant.core.agent.AgentState
import com.aether.assistant.core.agent.StepStatus
import com.aether.assistant.core.util.truncate
import com.aether.assistant.ui.theme.Amber
import com.aether.assistant.ui.theme.Cyan
import com.aether.assistant.ui.theme.Green
import com.aether.assistant.ui.theme.Panel
import com.aether.assistant.ui.theme.Red
import com.aether.assistant.ui.theme.TextMuted

@Composable
fun TaskProgressOverlay(state: AgentState) {
    val plan = state.plan
    AnimatedVisibility(
        visible = plan != null && state.running,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        if (plan == null) return@AnimatedVisibility
        val done = plan.steps.count { it.status == StepStatus.DONE || it.status == StepStatus.SKIPPED }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Panel.copy(alpha = 0.95f))
                .padding(14.dp)
        ) {
            Text("TASK EXECUTION", style = MaterialTheme.typography.labelSmall, color = Cyan)
            Spacer(Modifier.height(6.dp))
            Text(plan.goal.truncate(90), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { if (plan.steps.isEmpty()) 0f else done.toFloat() / plan.steps.size },
                modifier = Modifier.fillMaxWidth(),
                color = Cyan
            )
            Spacer(Modifier.height(10.dp))
            plan.steps.forEach { step ->
                val c = when (step.status) {
                    StepStatus.DONE -> Green
                    StepStatus.FAILED -> Red
                    StepStatus.RUNNING -> Amber
                    StepStatus.SKIPPED -> TextMuted
                    StepStatus.PENDING -> TextMuted
                }
                Row {
                    Text(
                        when (step.status) {
                            StepStatus.DONE -> "✔"
                            StepStatus.FAILED -> "✖"
                            StepStatus.RUNNING -> "▸"
                            StepStatus.SKIPPED -> "—"
                            StepStatus.PENDING -> "○"
                        },
                        color = c,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text(step.label.truncate(70), color = c, style = MaterialTheme.typography.bodyMedium)
                }
                if (step.detail.isNotBlank()) {
                    Text(step.detail.truncate(110), color = TextMuted, style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 20.dp))
                }
            }
        }
    }
}
