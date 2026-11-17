package com.aether.assistant.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aether.assistant.core.memory.CryptoBox
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("aether_settings")

enum class AiMode { OFFLINE, ONLINE }

data class AiEndpoint(
    val name: String,
    val baseUrl: String,
    val model: String,
    val token: String
)

data class AppSettings(
    val mode: AiMode = AiMode.OFFLINE,
    val endpoint: AiEndpoint = AiEndpoint("local-default", "http://127.0.0.1:8080/v1", "local-model", ""),
    val handsFree: Boolean = false,
    val screenIntelligence: Boolean = false,
    val notificationIntelligence: Boolean = false,
    val onlineVision: Boolean = false,
    val onlineFiles: Boolean = false,
    val confirmSensitive: Boolean = true,
    val autoTts: Boolean = true
)

class SettingsRepository(context: Context) {
    private val app = context.applicationContext
    private val crypto = CryptoBox(app)

    private object Keys {
        val MODE = stringPreferencesKey("mode")
        val ENDPOINT_NAME = stringPreferencesKey("endpoint_name")
        val ENDPOINT_URL = stringPreferencesKey("endpoint_url")
        val ENDPOINT_MODEL = stringPreferencesKey("endpoint_model")
        val ENDPOINT_TOKEN = stringPreferencesKey("endpoint_token_enc")
        val HANDS_FREE = booleanPreferencesKey("hands_free")
        val SCREEN = booleanPreferencesKey("screen_intelligence")
        val NOTIFICATIONS = booleanPreferencesKey("notification_intelligence")
        val ONLINE_VISION = booleanPreferencesKey("online_vision")
        val ONLINE_FILES = booleanPreferencesKey("online_files")
        val CONFIRM = booleanPreferencesKey("confirm_sensitive")
        val AUTO_TTS = booleanPreferencesKey("auto_tts")
    }

    val settings: Flow<AppSettings> = app.settingsDataStore.data.map { p ->
        AppSettings(
            mode = runCatching { AiMode.valueOf(p[Keys.MODE] ?: "OFFLINE") }.getOrDefault(AiMode.OFFLINE),
            endpoint = AiEndpoint(
                name = p[Keys.ENDPOINT_NAME] ?: "local-default",
                baseUrl = p[Keys.ENDPOINT_URL] ?: "http://127.0.0.1:8080/v1",
                model = p[Keys.ENDPOINT_MODEL] ?: "local-model",
                token = p[Keys.ENDPOINT_TOKEN]?.let { runCatching { crypto.decrypt(CryptoBox.EncryptedBlob.deserialize(it)) }.getOrDefault("") } ?: ""
            ),
            handsFree = p[Keys.HANDS_FREE] ?: false,
            screenIntelligence = p[Keys.SCREEN] ?: false,
            notificationIntelligence = p[Keys.NOTIFICATIONS] ?: false,
            onlineVision = p[Keys.ONLINE_VISION] ?: false,
            onlineFiles = p[Keys.ONLINE_FILES] ?: false,
            confirmSensitive = p[Keys.CONFIRM] ?: true,
            autoTts = p[Keys.AUTO_TTS] ?: true
        )
    }

    suspend fun setMode(mode: AiMode) = app.settingsDataStore.edit { it[Keys.MODE] = mode.name }

    suspend fun setEndpoint(endpoint: AiEndpoint) = app.settingsDataStore.edit { p ->
        p[Keys.ENDPOINT_NAME] = endpoint.name
        p[Keys.ENDPOINT_URL] = endpoint.baseUrl
        p[Keys.ENDPOINT_MODEL] = endpoint.model
        p[Keys.ENDPOINT_TOKEN] = if (endpoint.token.isBlank()) "" else crypto.encrypt(endpoint.token).serialize()
    }

    suspend fun setToggle(key: String, value: Boolean) = app.settingsDataStore.edit { p ->
        p[booleanPreferencesKey(key)] = value
    }

    suspend fun setHandsFree(v: Boolean) = setToggle("hands_free", v)
    suspend fun setScreen(v: Boolean) = setToggle("screen_intelligence", v)
    suspend fun setNotifications(v: Boolean) = setToggle("notification_intelligence", v)
    suspend fun setOnlineVision(v: Boolean) = setToggle("online_vision", v)
    suspend fun setOnlineFiles(v: Boolean) = setToggle("online_files", v)
    suspend fun setConfirm(v: Boolean) = setToggle("confirm_sensitive", v)
    suspend fun setAutoTts(v: Boolean) = setToggle("auto_tts", v)
}
