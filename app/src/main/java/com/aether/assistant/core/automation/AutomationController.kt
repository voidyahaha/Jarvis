package com.aether.assistant.core.automation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

/** High-level device actions using official APIs + accessibility where needed. */
class AutomationController(private val context: Context) {

    fun openApp(query: String): Result<String> = runCatching {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(0)
        val match = apps.firstOrNull { app ->
            val label = pm.getApplicationLabel(app).toString()
            label.contains(query, ignoreCase = true) || app.packageName.contains(query, ignoreCase = true)
        } ?: error("No installed app matches '$query'")

        val launch = pm.getLaunchIntentForPackage(match.packageName)
            ?: error("${match.packageName} has no launchable activity")
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launch)
        "Opened ${pm.getApplicationLabel(match)}"
    }

    fun openWeb(query: String) {
        val url = if (query.contains(".")) "https://$query" else "https://duckduckgo.com/?q=${Uri.encode(query)}"
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openSettings() {
        context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun alarm(hour: Int, minute: Int, label: String) {
        val i = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(i)
    }

    fun click(text: String): Result<String> = runCatching {
        require(AetherAccessibilityService.isReady()) { "Accessibility service is not enabled" }
        if (AetherAccessibilityService.instance?.clickText(text) == true) "Tapped \"$text\""
        else error("Could not find \"$text\" on screen")
    }

    fun type(text: String): Result<String> = runCatching {
        require(AetherAccessibilityService.isReady()) { "Accessibility service is not enabled" }
        if (AetherAccessibilityService.instance?.typeIntoFocused(text) == true) "Typed text"
        else error("No focused input field")
    }

    fun scroll(): Result<String> = runCatching {
        require(AetherAccessibilityService.isReady()) { "Accessibility service is not enabled" }
        val svc = AetherAccessibilityService.instance ?: error("Accessibility service unavailable")
        val root = svc.rootInActiveWindow ?: error("No active window")
        val scrollable = findScrollable(root)
        val ok = scrollable?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) ?: false
        if (ok) "Scrolled" else error("No scrollable element found")
    }

    private fun findScrollable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable && node.className?.toString()?.contains("ScrollView", true) == true) return node
        for (i in 0 until node.childCount) {
            findScrollable(node.getChild(i))?.let { return it }
        }
        return null
    }

    fun screenText(): Result<String> = runCatching {
        require(AetherAccessibilityService.isReady()) { "Accessibility service is not enabled" }
        AetherAccessibilityService.instance?.describeScreen() ?: error("No active window")
    }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
