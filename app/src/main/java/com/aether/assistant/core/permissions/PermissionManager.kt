package com.aether.assistant.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

enum class PermissionState { GRANTED, DENIED, NOT_GRANTED }

data class CapabilityPermission(
    val label: String,
    val state: PermissionState,
    val explanation: String,
    val permission: String? = null,
    val settingsAction: String? = null
)

class PermissionManager(private val context: Context) {
    private val pm = context.packageManager

    fun runtimeGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun accessibilityEnabled(): Boolean {
        val expected = context.packageName + "/.core.automation.AetherAccessibilityService"
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    fun notificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return flat.split(':').any { it.contains(context.packageName, ignoreCase = true) }
    }

    fun canDrawOverlays(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true

    fun snapshot(): List<CapabilityPermission> = listOf(
        CapabilityPermission(
            "🎙️ Microphone",
            state(runtimeGranted(Manifest.permission.RECORD_AUDIO)),
            "Needed for voice conversation. Audio is processed on-device unless you explicitly choose Online AI.",
            Manifest.permission.RECORD_AUDIO
        ),
        CapabilityPermission(
            "📷 Camera",
            state(runtimeGranted(Manifest.permission.CAMERA)),
            "Used only when you open Vision or a scan flow. A visible indicator shows when it is active.",
            Manifest.permission.CAMERA
        ),
        CapabilityPermission(
            "👁️ Screen capture",
            state(pm.hasFeature(PackageManager.FEATURE_SCREEN_CAPTURE) && canDrawOverlays()),
            "MediaProjection permission is requested each session. Required only for screen understanding.",
            settingsAction = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
        ),
        CapabilityPermission(
            "🤖 Accessibility",
            state(accessibilityEnabled()),
            "Required for clicking, typing and scrolling on your behalf. You can turn it off any time.",
            settingsAction = Settings.ACTION_ACCESSIBILITY_SETTINGS
        ),
        CapabilityPermission(
            "🔔 Notifications",
            state(notificationListenerEnabled()),
            "Optional. Notification text is never sent online unless you explicitly allow it.",
            settingsAction = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
        ),
        CapabilityPermission(
            "📁 Files",
            state(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    runtimeGranted(Manifest.permission.READ_MEDIA_IMAGES)
                else runtimeGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
            ),
            "Files are picked through the Android picker. Nothing is uploaded without your consent.",
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        ),
        CapabilityPermission(
            "🌐 Network / Online AI",
            state(runtimeGranted(Manifest.permission.INTERNET)),
            "Online processing only happens when you switch to Online AI or explicitly permit a web action."
        )
    )

    private fun state(granted: Boolean) =
        if (granted) PermissionState.GRANTED else PermissionState.NOT_GRANTED
}
