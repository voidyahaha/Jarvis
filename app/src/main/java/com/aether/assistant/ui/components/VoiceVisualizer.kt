package com.aether.assistant.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

/** Animated HUD voice orb. Pure Compose — no media or visual-effects SDK. */
@Composable
fun VoiceVisualizer(
    modifier: Modifier,
    active: Boolean,
    speaking: Boolean,
    color: Color
) {
    val transition = rememberInfiniteTransition(label = "voice")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = 6.28f,
        animationSpec = infiniteRepeatable(tween(if (active) 1400 else 4200), RepeatMode.Restart),
        label = "phase"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.85f, targetValue = if (active) 1.25f else 1.02f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse"
    )

    Canvas(modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val base = minOf(size.width, size.height) * 0.22f * pulse
        drawCircle(color.copy(alpha = 0.12f), radius = base * 1.9f)
        drawCircle(color.copy(alpha = 0.25f), radius = base * 1.5f, style = Stroke(1.5f))

        val bars = if (active || speaking) 48 else 24
        for (i in 0 until bars) {
            val a = phase + (i * 6.28f / bars)
            val wobble = if (active || speaking) (sin(a * 3) + cos(a * 5)) * 8f else 2f
            val r1 = base + wobble
            val r2 = r1 + 10 + (i % 5) * 2
            drawLine(
                color = color.copy(alpha = if (i % 3 == 0) 0.9f else 0.45f),
                start = Offset(cx + cos(a) * r1, cy + sin(a) * r1),
                end = Offset(cx + cos(a) * r2, cy + sin(a) * r2),
                strokeWidth = if (i % 3 == 0) 3f else 1.5f
            )
        }
        drawCircle(color, radius = base * 0.55f, style = Stroke(2f))
    }
}
