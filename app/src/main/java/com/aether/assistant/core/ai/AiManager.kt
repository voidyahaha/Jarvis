package com.aether.assistant.core.ai

import com.aether.assistant.core.settings.AiEndpoint
import com.aether.assistant.core.settings.AiMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Chooses the active engine. Never silently uploads: engine choice is explicit. */
class AiManager {
    private val _mode = MutableStateFlow(AiMode.OFFLINE)
    val mode: StateFlow<AiMode> = _mode

    private val online = MutableStateFlow<AiEngine?>(null)

    fun configure(mode: AiMode, endpoint: AiEndpoint, local: AiEngine) {
        _mode.value = mode
        online.value = if (mode == AiMode.ONLINE) OnlineEngine(endpoint) else null
    }

    fun active(local: AiEngine): AiEngine = when (_mode.value) {
        AiMode.OFFLINE -> local
        AiMode.ONLINE -> online.value ?: local
    }

    fun statusLabel(local: AiEngine): String = active(local).label()
}
