package com.aether.assistant.core.automation

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Optional notification reader. Contents stay in memory unless user exports them. */
class AetherNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        _connected.value = true
    }

    override fun onListenerDisconnected() {
        _connected.value = false
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val n = sbn ?: return
        val title = n.notification.extras?.getCharSequence("android.title")?.toString().orEmpty()
        val text = n.notification.extras?.getCharSequence("android.text")?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return
        _recent.value = ("${n.packageName}: $title — $text").take(400)
    }

    companion object {
        private val _connected = MutableStateFlow(false)
        val connected: StateFlow<Boolean> = _connected
        private val _recent = MutableStateFlow("(none yet)")
        val recent: StateFlow<String> = _recent
    }
}
