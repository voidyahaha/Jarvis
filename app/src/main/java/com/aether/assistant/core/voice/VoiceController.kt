package com.aether.assistant.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

data class VoiceState(
    val listening: Boolean = false,
    val speaking: Boolean = false,
    val partial: String = "",
    val lastError: String? = null
)

/**
 * Android SpeechRecognizer + TextToSpeech. On-device recognition is used when
 * the system exposes it; no paid voice API is ever contacted by this class.
 */
class VoiceController(context: Context) {
    private val app = context.applicationContext
    private val _state = MutableStateFlow(VoiceState())
    val state: StateFlow<VoiceState> = _state

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var onFinal: ((String) -> Unit)? = null
    private var interrupted = false

    init {
        tts = TextToSpeech(app) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) {
                        _state.value = _state.value.copy(speaking = true)
                    }
                    override fun onDone(id: String?) {
                        _state.value = _state.value.copy(speaking = false)
                    }
                    override fun onError(id: String?) {
                        _state.value = _state.value.copy(speaking = false)
                    }
                })
            }
        }
    }

    fun startListening(onFinalResult: (String) -> Unit) {
        onFinal = onFinalResult
        if (!SpeechRecognizer.isRecognitionAvailable(app)) {
            _state.value = _state.value.copy(lastError = "Speech recognition is not available on this device")
            return
        }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(app).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _state.value = _state.value.copy(listening = true, lastError = null)
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _state.value = _state.value.copy(listening = false)
                    }
                    override fun onError(error: Int) {
                        _state.value = _state.value.copy(
                            listening = false,
                            lastError = "Voice error code: $error"
                        )
                    }
                    override fun onResults(results: Bundle?) {
                        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull().orEmpty()
                        _state.value = _state.value.copy(listening = false, partial = "")
                        if (text.isNotBlank()) onFinal?.invoke(text)
                    }
                    override fun onPartialResults(partial: Bundle?) {
                        val text = partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull().orEmpty()
                        _state.value = _state.value.copy(partial = text)
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Prefer on-device recognition when the OS supports it.
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        recognizer?.startListening(intent)
    }

    fun stopListening() {
        recognizer?.stopListening()
        _state.value = _state.value.copy(listening = false)
    }

    /** Speak with interruption support: calling this again cancels current speech. */
    fun speak(text: String) {
        if (text.isBlank()) return
        interrupted = false
        tts?.stop()
        val id = "aether_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    fun interrupt() {
        interrupted = true
        tts?.stop()
        recognizer?.stopListening()
        _state.value = _state.value.copy(speaking = false, listening = false)
    }

    fun shutdown() {
        recognizer?.destroy()
        tts?.shutdown()
    }
}
