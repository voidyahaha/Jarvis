package com.aether.assistant.core.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Real UI automation primitive. Every action here is invoked only by the agent
 * after the user's request (and after confirmation for sensitive actions).
 */
class AetherAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        serviceInfo = serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        _connected.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Kept for observability; the agent reads the tree on demand.
        _lastEvent.value = event?.className?.toString().orEmpty()
    }

    override fun onInterrupt() {
        _connected.value = false
    }

    override fun onDestroy() {
        instance = null
        _connected.value = false
        super.onDestroy()
    }

    /** Dump readable text + resource ids of the current window. */
    fun describeScreen(): String {
        val root = rootInActiveWindow ?: return "(no active window)"
        val out = StringBuilder()
        collect(root, out, 0)
        return out.toString().take(8000)
    }

    private fun collect(node: AccessibilityNodeInfo?, out: StringBuilder, depth: Int) {
        if (node == null || depth > 14) return
        val label = node.text?.toString().orEmpty()
            .ifBlank { node.contentDescription?.toString().orEmpty() }
        val id = node.viewIdResourceName.orEmpty()
        if (label.isNotBlank() || id.isNotBlank()) {
            out.append("  ".repeat(depth))
                .append("[${node.className ?: "view"}] ")
                .append(if (label.isNotBlank()) "\"$label\" " else "")
                .append(if (id.isNotBlank()) "id=$id" else "")
                .append('\n')
        }
        for (i in 0 until node.childCount) collect(node.getChild(i), out, depth + 1)
    }

    fun clickText(target: String, exact: Boolean = false): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(target)
        val node = nodes.firstOrNull { n ->
            val t = (n.text?.toString().orEmpty() + n.contentDescription?.toString().orEmpty())
            if (exact) t.equals(target, ignoreCase = true) else t.contains(target, ignoreCase = true)
        } ?: return false
        val ok = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        if (!ok) {
            var p = node.parent
            while (p != null) {
                if (p.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
                p = p.parent
            }
        }
        return ok
    }

    fun clickId(resourceId: String): Boolean {
        val root = rootInActiveWindow ?: return false
        return root.findAccessibilityNodeInfosByViewId(resourceId)
            .firstOrNull()?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }

    fun typeIntoFocused(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args) ||
            focused.performAction(AccessibilityNodeInfo.ACTION_PASTE, args)
    }

    fun scrollForward(): Boolean {
        val root = rootInActiveWindow ?: return false
        return findScrollable(root)?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) ?: false
    }

    fun scrollBackward(): Boolean {
        val root = rootInActiveWindow ?: return false
        return findScrollable(root)?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) ?: false
    }

    private fun findScrollable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            findScrollable(node.getChild(i))?.let { return it }
        }
        return null
    }

    fun back(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun home(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun recents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)

    companion object {
        @Volatile var instance: AetherAccessibilityService? = null
            private set
        private val _connected = MutableStateFlow(false)
        val connected: StateFlow<Boolean> = _connected
        fun isReady() = instance != null && _connected.value
    }
}
