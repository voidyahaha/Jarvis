package com.aether.assistant.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkScheme = darkColorScheme(
    primary = Cyan,
    onPrimary = Void,
    secondary = Violet,
    background = Void,
    onBackground = TextPrimary,
    surface = Panel,
    onSurface = TextPrimary,
    error = Red
)

private val LightScheme = lightColorScheme(
    primary = CyanDim,
    background = Void,
    onBackground = TextPrimary,
    surface = Panel,
    onSurface = TextPrimary
)

@Composable
fun AetherTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkScheme else LightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Void.toArgb()
            window.navigationBarColor = Void.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(colorScheme = colors, typography = AetherTypography, content = content)
}
