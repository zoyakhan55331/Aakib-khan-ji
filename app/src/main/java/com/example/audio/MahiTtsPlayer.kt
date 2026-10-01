package com.example.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class MahiTtsPlayer(
    context: Context,
    private val onSpeechStarted: () -> Unit,
    private val onSpeechCompleted: () -> Unit,
    private val onSpeechError: (String) -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var currentPitch = 1.15f
    private var currentRate = 1.05f

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            setupVoice()
        } else {
            Log.e("MahiTtsPlayer", "TextToSpeech init failed with status: $status")
            onSpeechError("TTS init failed")
        }
    }

    private fun setupVoice() {
        val tts = this.tts ?: return

        // Prefer Indian English or Hindi
        val inLocale = Locale.forLanguageTag("en-IN")
        val hiLocale = Locale.forLanguageTag("hi-IN")

        if (tts.isLanguageAvailable(inLocale) >= TextToSpeech.LANG_AVAILABLE) {
            tts.language = inLocale
        } else if (tts.isLanguageAvailable(hiLocale) >= TextToSpeech.LANG_AVAILABLE) {
            tts.language = hiLocale
        } else {
            tts.language = Locale.ENGLISH
        }

        // Try to pick a female voice if available
        try {
            val voices = tts.voices
            if (voices != null) {
                val femaleVoice = voices.firstOrNull { voice ->
                    val name = voice.name.lowercase()
                    (name.contains("female") || name.contains("female") || name.contains("in-language")) &&
                            (voice.locale.language == "hi" || voice.locale.country == "IN" || voice.locale.language == "en")
                } ?: voices.firstOrNull { it.name.lowercase().contains("female") }

                if (femaleVoice != null) {
                    tts.voice = femaleVoice
                }
            }
        } catch (_: Exception) {}

        tts.setPitch(currentPitch)
        tts.setSpeechRate(currentRate)

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
                onSpeechStarted()
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                onSpeechCompleted()
            }

            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                onSpeechError("Speech synthesis error")
            }
        })
    }

    fun updateSettings(pitch: Float, rate: Float) {
        currentPitch = pitch
        currentRate = rate
        tts?.setPitch(pitch)
        tts?.setSpeechRate(rate)
    }

    fun speak(text: String, utteranceId: String = System.currentTimeMillis().toString()) {
        if (!isInitialized || tts == null) {
            Log.w("MahiTtsPlayer", "TTS not initialized yet")
            return
        }

        // Clean any emojis before feeding to TTS for smoother speech
        val cleanedText = text.replace(Regex("[\\p{So}\\p{Cn}]"), "").trim()
        if (cleanedText.isBlank()) return

        stop()

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        tts?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        if (_isSpeaking.value || tts?.isSpeaking == true) {
            tts?.stop()
            _isSpeaking.value = false
        }
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
    }
}
