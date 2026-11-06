package com.wakemethere.app.alarm

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Helper class that provides high-quality, natural-sounding voice arrival announcements.
 *
 * Uses Android's native offline/neural TextToSpeech voices configured for the USAGE_ALARM
 * audio stream so announcements are loud and crystal-clear even if the device is on silent.
 */
class VoiceAnnouncementHelper(
    private val context: Context,
    private val onDuckingStateChanged: ((isSpeaking: Boolean) -> Unit)? = null
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var repeatJob: Job? = null
    private val helperScope = CoroutineScope(Dispatchers.Main)

    private var onSingleUtteranceFinished: (() -> Unit)? = null

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                configureNaturalVoice()
                setupAudioAttributes()
                setupProgressListener()
                Log.d(TAG, "TextToSpeech successfully initialized with natural voice settings")
            } else {
                Log.e(TAG, "Failed to initialize TextToSpeech: status=$status")
            }
        }
    }

    /**
     * Inspects available device voices and selects the highest quality neural/natural local voice.
     */
    private fun configureNaturalVoice() {
        val ttsInstance = tts ?: return
        val currentLocale = Locale.getDefault()

        // Set device locale, fall back to English if unsupported
        val result = ttsInstance.setLanguage(currentLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            ttsInstance.setLanguage(Locale.US)
        }

        try {
            val availableVoices = ttsInstance.voices
            if (!availableVoices.isNullOrEmpty()) {
                val targetLang = ttsInstance.voice?.locale?.language ?: currentLocale.language
                // Find best local high-quality voice
                val naturalVoice = availableVoices
                    .filter { it.locale.language.equals(targetLang, ignoreCase = true) }
                    .maxWithOrNull(
                        compareBy<Voice> { !it.isNetworkConnectionRequired }
                            .thenBy { it.quality }
                            .thenBy { it.latency }
                    )
                if (naturalVoice != null) {
                    ttsInstance.voice = naturalVoice
                    Log.d(TAG, "Selected natural voice: ${naturalVoice.name} (quality=${naturalVoice.quality})")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to inspect voice list, using default voice engine", e)
        }

        // Set cadence for a clear, natural alert delivery
        ttsInstance.setPitch(1.0f)
        ttsInstance.setSpeechRate(0.95f)
    }

    private fun setupAudioAttributes() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        tts?.setAudioAttributes(audioAttributes)
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                onDuckingStateChanged?.invoke(true)
            }

            override fun onDone(utteranceId: String?) {
                onDuckingStateChanged?.invoke(false)
                onSingleUtteranceFinished?.invoke()
                onSingleUtteranceFinished = null
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                onDuckingStateChanged?.invoke(false)
                onSingleUtteranceFinished?.invoke()
                onSingleUtteranceFinished = null
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e(TAG, "Utterance error: $errorCode for id: $utteranceId")
                onDuckingStateChanged?.invoke(false)
                onSingleUtteranceFinished?.invoke()
                onSingleUtteranceFinished = null
            }
        })
    }

    /**
     * Loops the destination arrival announcement periodically (every [intervalSeconds] seconds)
     * until [stop] or [shutdown] is called.
     */
    fun startRepeatingAnnouncement(
        destinationName: String,
        style: Int = 0,
        intervalSeconds: Long = 9L
    ) {
        stopRepeating()
        val announcementText = getAnnouncementText(destinationName, style)

        repeatJob = helperScope.launch {
            // Await initialization if necessary
            var retries = 0
            while (!isInitialized && retries < 25) {
                delay(200)
                retries++
            }

            while (isActive) {
                speakUtterance(announcementText)
                // Wait interval before repeating
                delay(intervalSeconds * 1000L)
            }
        }
    }

    /**
     * Speaks once (useful for previewing inside the UI configuration screen).
     */
    fun speakOnce(text: String, onFinished: (() -> Unit)? = null) {
        stopRepeating()
        this.onSingleUtteranceFinished = onFinished
        helperScope.launch {
            var retries = 0
            while (!isInitialized && retries < 25) {
                delay(200)
                retries++
            }
            speakUtterance(text)
        }
    }

    private fun speakUtterance(text: String) {
        val ttsInstance = tts ?: return
        val utteranceId = "WakeAnnouncement_${System.currentTimeMillis()}"

        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioAttributes.USAGE_ALARM)
        }

        ttsInstance.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stopRepeating() {
        repeatJob?.cancel()
        repeatJob = null
        tts?.stop()
        onDuckingStateChanged?.invoke(false)
        onSingleUtteranceFinished?.invoke()
        onSingleUtteranceFinished = null
    }

    fun shutdown() {
        stopRepeating()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    companion object {
        private const val TAG = "VoiceAnnouncementHelper"

        fun getAnnouncementText(destinationName: String, style: Int): String {
            val dest = destinationName.trim().ifBlank { "Destination" }
            return when (style) {
                0 -> "Wake up! You have reached $dest! Wake up, you have arrived!"
                1 -> "Attention! Arriving at $dest. Please prepare to get off."
                2 -> "You have reached your destination: $dest."
                else -> "Wake up! You have reached $dest! Wake up!"
            }
        }

        val STYLES = listOf(
            0 to Pair("Wake-Up Alert", "Wake up! You have reached [Location]! Wake up, you have arrived!"),
            1 to Pair("Transit Commute", "Attention! Arriving at [Location]. Please prepare to get off."),
            2 to Pair("Calm Reminder", "You have reached your destination: [Location].")
        )

        fun getStyleShortLabel(style: Int): String {
            return when (style) {
                0 -> "Wake-Up"
                1 -> "Transit"
                2 -> "Calm"
                else -> "Style $style"
            }
        }
    }
}

