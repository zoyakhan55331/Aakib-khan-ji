package com.example.live

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.example.ai.GeminiClient
import com.example.ai.GeminiResponse
import com.example.ai.MahiLocalConversationEngine
import com.example.ai.MahiPersonality
import com.example.audio.AudioRecordStreamer
import com.example.audio.MahiSpeechRecognizer
import com.example.audio.MahiTtsPlayer
import com.example.data.MemoryRepository
import com.example.device.ActionResult
import com.example.device.ActionStatus
import com.example.device.DeviceActionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class VoiceState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    LISTENING,
    THINKING,
    SPEAKING,
    INTERRUPTED,
    ERROR
}

data class ActionBannerData(
    val title: String,
    val isSuccess: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class VoiceStateManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val memoryRepo: MemoryRepository,
    private val deviceManager: DeviceActionManager
) {
    private val _voiceState = MutableStateFlow(VoiceState.DISCONNECTED)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    private val _userTranscript = MutableStateFlow("")
    val userTranscript: StateFlow<String> = _userTranscript.asStateFlow()

    private val _mahiResponse = MutableStateFlow("Hi! Main hoon Mahi. Tap the orb to talk to me! ✨")
    val mahiResponse: StateFlow<String> = _mahiResponse.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _actionBanner = MutableStateFlow<ActionBannerData?>(null)
    val actionBanner: StateFlow<ActionBannerData?> = _actionBanner.asStateFlow()

    private val vibrator: Vibrator? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(android.os.VibratorManager::class.java)
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val geminiClient = GeminiClient()
    private val localEngine = MahiLocalConversationEngine(deviceManager)

    private var speechRecognizer: MahiSpeechRecognizer? = null
    private var ttsPlayer: MahiTtsPlayer? = null
    private var audioStreamer: AudioRecordStreamer? = null

    private var isContinuousSession = true
    private var processingJob: Job? = null

    init {
        setupTts()
        setupSpeechRecognizer()
        setupAudioStreamer()
    }

    private fun setupTts() {
        ttsPlayer = MahiTtsPlayer(
            context = context,
            onSpeechStarted = {
                _voiceState.value = VoiceState.SPEAKING
            },
            onSpeechCompleted = {
                if (_isSessionActive.value && isContinuousSession) {
                    _voiceState.value = VoiceState.LISTENING
                    startListeningInternal()
                } else if (_isSessionActive.value) {
                    _voiceState.value = VoiceState.CONNECTED
                }
            },
            onSpeechError = { errorMsg ->
                Log.e("VoiceStateManager", "TTS Error: $errorMsg")
                if (_isSessionActive.value) {
                    _voiceState.value = VoiceState.LISTENING
                    startListeningInternal()
                }
            }
        )
        ttsPlayer?.updateSettings(memoryRepo.speechPitch, memoryRepo.speechRate)
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = MahiSpeechRecognizer(
            context = context,
            onPartialResultCallback = { partial ->
                _userTranscript.value = partial
            },
            onFinalResultCallback = { finalQuery ->
                _userTranscript.value = finalQuery
                processUserQuery(finalQuery)
            },
            onRmsUpdateCallback = { rms ->
                if (_voiceState.value == VoiceState.LISTENING) {
                    _audioAmplitude.value = rms
                }
            },
            onErrorCallback = { error ->
                Log.w("VoiceStateManager", "SpeechRecognizer error: $error")
                if (_voiceState.value == VoiceState.LISTENING && _isSessionActive.value) {
                    // Slight backoff and retry listening if session is still active
                    scope.launch {
                        delay(600)
                        if (_isSessionActive.value && _voiceState.value == VoiceState.LISTENING) {
                            startListeningInternal()
                        }
                    }
                }
            }
        )
    }

    private fun setupAudioStreamer() {
        audioStreamer = AudioRecordStreamer(
            onSpeechDetected = {
                // If user speaks while Mahi is speaking -> INTERRUPT!
                if (_voiceState.value == VoiceState.SPEAKING) {
                    interrupt()
                }
            }
        )
    }

    fun startSession() {
        _isSessionActive.value = true
        _voiceState.value = VoiceState.CONNECTING
        triggerHaptic()

        scope.launch {
            audioStreamer?.startStreaming(scope)
            delay(200)
            _voiceState.value = VoiceState.LISTENING
            startListeningInternal()
        }
    }

    fun stopSession() {
        _isSessionActive.value = false
        _voiceState.value = VoiceState.DISCONNECTED
        processingJob?.cancel()
        ttsPlayer?.stop()
        speechRecognizer?.stopListening()
        audioStreamer?.stopStreaming()
        _audioAmplitude.value = 0f
        triggerHaptic()
    }

    fun toggleSession() {
        if (_isSessionActive.value) {
            // If Mahi is speaking, first tap interrupts speech and starts listening
            if (_voiceState.value == VoiceState.SPEAKING) {
                interrupt()
            } else {
                stopSession()
            }
        } else {
            startSession()
        }
    }

    fun interrupt() {
        if (_voiceState.value == VoiceState.SPEAKING) {
            _voiceState.value = VoiceState.INTERRUPTED
            ttsPlayer?.stop()
            triggerHaptic()
            scope.launch {
                delay(150)
                if (_isSessionActive.value) {
                    _voiceState.value = VoiceState.LISTENING
                    startListeningInternal()
                }
            }
        }
    }

    private fun startListeningInternal() {
        try {
            speechRecognizer?.startListening(memoryRepo.languagePreference)
        } catch (e: Exception) {
            Log.e("VoiceStateManager", "Error starting listening", e)
        }
    }

    fun submitTextQuery(query: String) {
        if (query.isBlank()) return
        _userTranscript.value = query
        if (!_isSessionActive.value) {
            _isSessionActive.value = true
        }
        processUserQuery(query)
    }

    private fun processUserQuery(query: String) {
        speechRecognizer?.stopListening()
        _voiceState.value = VoiceState.THINKING
        triggerHaptic()

        processingJob?.cancel()
        processingJob = scope.launch(Dispatchers.IO) {
            try {
                // Record user message
                memoryRepo.recordConversation("user", query)

                // Retrieve memory context
                val memoryContext = memoryRepo.getFormattedMemoryContext()
                val recentHistory = memoryRepo.getRecentConversations(8).first()
                val historyPairs = recentHistory.reversed().map { it.sender to it.text }

                val systemPrompt = MahiPersonality.getSystemPrompt(
                    userName = null,
                    sassLevel = memoryRepo.sassLevel,
                    memoryContext = memoryContext
                )

                // Try real Gemini API first
                val geminiResult = geminiClient.generateMahiResponse(
                    userMessage = query,
                    systemPrompt = systemPrompt,
                    conversationHistory = historyPairs
                )

                when (geminiResult) {
                    is GeminiResponse.TextResponse -> {
                        // Natural conversation! NEVER show action banner
                        _actionBanner.value = null
                        speakAndShowResponse(geminiResult.text)
                    }

                    is GeminiResponse.FunctionCallResponse -> {
                        // Device action execution
                        handleGeminiFunctionCall(geminiResult)
                    }

                    is GeminiResponse.Error -> {
                        // Network or API key missing -> Seamless fallback to MahiLocalConversationEngine
                        val localResult = localEngine.processQuery(
                            rawQuery = query,
                            sassLevel = memoryRepo.sassLevel
                        )

                        // Save any extracted memory
                        localResult.memoryToSave?.let { (cat, k, v) ->
                            memoryRepo.saveMemory(cat, k, v)
                        }

                        if (localResult.isDeviceAction && localResult.actionResult != null) {
                            showActionBanner(
                                localResult.actionResult.userFriendlyMessage,
                                localResult.actionResult.status == ActionStatus.SUCCESS
                            )
                        } else {
                            _actionBanner.value = null
                        }

                        speakAndShowResponse(localResult.spokenResponse, localResult.actionResult?.userFriendlyMessage)
                    }
                }
            } catch (e: Exception) {
                Log.e("VoiceStateManager", "Query processing error", e)
                val fallback = "Arre kuch network glitch hua lagta hai. Phir se bolo zara? 😏"
                speakAndShowResponse(fallback)
            }
        }
    }

    private suspend fun handleGeminiFunctionCall(call: GeminiResponse.FunctionCallResponse) {
        val args = call.arguments
        var actionResult: ActionResult? = null

        when (call.functionName) {
            "open_app" -> {
                val appName = args["app_name"]?.toString() ?: ""
                actionResult = deviceManager.openApp(appName)
            }
            "open_website" -> {
                val url = args["url"]?.toString() ?: ""
                actionResult = deviceManager.openWebsite(url)
            }
            "search_web" -> {
                val q = args["query"]?.toString() ?: ""
                actionResult = deviceManager.searchWeb(q)
            }
            "open_settings" -> {
                val type = args["setting_type"]?.toString() ?: "general"
                actionResult = deviceManager.openSettings(type)
            }
            "make_phone_call" -> {
                val target = args["contact_or_number"]?.toString() ?: ""
                actionResult = deviceManager.makePhoneCall(target)
            }
            "send_sms" -> {
                val recipient = args["recipient"]?.toString() ?: ""
                val msg = args["message"]?.toString() ?: ""
                actionResult = deviceManager.sendSms(recipient, msg)
            }
            "set_alarm" -> {
                val hour = (args["hour"] as? Number)?.toInt() ?: 7
                val minute = (args["minute"] as? Number)?.toInt() ?: 0
                val label = args["label"]?.toString() ?: "Mahi Alarm"
                actionResult = deviceManager.setAlarm(hour, minute, label)
            }
            "set_timer" -> {
                val seconds = (args["duration_seconds"] as? Number)?.toInt() ?: 300
                val label = args["label"]?.toString() ?: "Mahi Timer"
                actionResult = deviceManager.setTimer(seconds, label)
            }
            "control_media" -> {
                val act = args["action"]?.toString() ?: "play"
                actionResult = deviceManager.controlMedia(act)
            }
            "save_memory" -> {
                val cat = args["category"]?.toString() ?: "PREFERENCES"
                val k = args["key"]?.toString() ?: "user_info"
                val v = args["value"]?.toString() ?: ""
                memoryRepo.saveMemory(cat, k, v)
                actionResult = ActionResult(
                    status = ActionStatus.SUCCESS,
                    actionName = "save_memory",
                    userFriendlyMessage = "Saved to memory: $k = $v",
                    spokenSummary = "Maine yeh yaad rakh liya hai boss! ✨"
                )
            }
        }

        if (actionResult != null) {
            showActionBanner(actionResult.userFriendlyMessage, actionResult.status == ActionStatus.SUCCESS)
            val spokenText = if (!call.preSpokenText.isNullOrBlank()) {
                call.preSpokenText + " " + actionResult.spokenSummary
            } else {
                actionResult.spokenSummary
            }
            speakAndShowResponse(spokenText, actionResult.userFriendlyMessage)
        } else {
            val reply = call.preSpokenText ?: "Done!"
            speakAndShowResponse(reply)
        }
    }

    private fun showActionBanner(message: String, isSuccess: Boolean) {
        _actionBanner.value = ActionBannerData(message, isSuccess)
        scope.launch {
            delay(4000)
            if (_actionBanner.value?.title == message) {
                _actionBanner.value = null
            }
        }
    }

    private suspend fun speakAndShowResponse(text: String, actionTaken: String? = null) {
        _mahiResponse.value = text
        memoryRepo.recordConversation("mahi", text, actionTaken)
        withContext(Dispatchers.Main) {
            ttsPlayer?.updateSettings(memoryRepo.speechPitch, memoryRepo.speechRate)
            ttsPlayer?.speak(text)
        }
    }

    private fun triggerHaptic() {
        try {
            vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {}
    }

    fun release() {
        stopSession()
        ttsPlayer?.shutdown()
        speechRecognizer?.destroyRecognizer()
        audioStreamer?.stopStreaming()
    }
}
