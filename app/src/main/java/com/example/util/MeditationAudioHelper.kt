package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

enum class MeditationAudioMode {
    TTS,
    AUDIO_FILE
}

/**
 * Controller for Voice-Guided Meditation.
 * Supports:
 * - Mode A: Text-to-Speech (TTS) calm instruction pacing, adjustable speech rate, voice preview, pause/resume/stop.
 * - Mode B: Prerecorded guided audio files via MediaPlayer with seek, duration, pause/resume/stop.
 * - Handles Audio Focus politely so music or external sounds are ducked or paused.
 */
class MeditationAudioHelper(private val context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    // TTS state
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var isTtsSpeaking = false
    private var currentStepIndex = 0
    private var stepsToSpeak: List<String> = emptyList()
    private var speechRate: Float = 0.85f // Calm, mindful pace by default
    private var stepIntervalSeconds: Int = 12

    // Audio file player state
    private var mediaPlayer: MediaPlayer? = null
    private var audioUri: Uri? = null

    // Audio focus
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    // Callbacks
    var onStepSpoken: ((Int, String) -> Unit)? = null
    var onSessionComplete: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onAudioProgress: ((Int, Int) -> Unit)? = null // (positionMs, durationMs)

    private val isPaused = AtomicBoolean(false)
    private val isStopped = AtomicBoolean(false)
    private var currentUtteranceId: String? = null

    init {
        audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        initializeTts()
    }

    private fun initializeTts() {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { engine ->
                    val result = engine.setLanguage(Locale.getDefault())
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        engine.language = Locale.US
                    }
                    val audioAttrs = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                    engine.setAudioAttributes(audioAttrs)
                    engine.setSpeechRate(speechRate)

                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            isTtsSpeaking = true
                        }

                        override fun onDone(utteranceId: String?) {
                            isTtsSpeaking = false
                            if (!isPaused.get() && !isStopped.get()) {
                                scheduleNextStep()
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            isTtsSpeaking = false
                            if (!isPaused.get() && !isStopped.get()) {
                                scheduleNextStep()
                            }
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            isTtsSpeaking = false
                            if (!isPaused.get() && !isStopped.get()) {
                                scheduleNextStep()
                            }
                        }
                    })
                    isTtsInitialized = true
                }
            } else {
                Log.w("MeditationAudioHelper", "TextToSpeech init failed")
                isTtsInitialized = false
            }
        }
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.6f, 1.5f)
        if (isTtsInitialized) {
            tts?.setSpeechRate(speechRate)
        }
    }

    fun previewVoice(sampleText: String = "Take a gentle breath in, and let your shoulders drop as you breathe out.") {
        if (!isTtsInitialized || tts == null) {
            onError?.invoke("Speech engine initializing, please try again in a moment.")
            return
        }
        requestAudioFocus()
        val utteranceId = "meditation_preview_${System.currentTimeMillis()}"
        tts?.setSpeechRate(speechRate)
        tts?.speak(sampleText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    /**
     * Start Mode A: TTS Guided Meditation
     */
    fun startTtsSession(steps: List<String>, intervalSeconds: Int = 12) {
        if (steps.isEmpty()) return
        stop()

        stepsToSpeak = steps
        currentStepIndex = 0
        stepIntervalSeconds = maxOf(4, intervalSeconds)
        isPaused.set(false)
        isStopped.set(false)

        requestAudioFocus()
        speakCurrentStep()
    }

    private fun speakCurrentStep() {
        if (isStopped.get() || isPaused.get()) return
        if (currentStepIndex >= stepsToSpeak.size) {
            onSessionComplete?.invoke()
            return
        }

        val text = stepsToSpeak[currentStepIndex]
        onStepSpoken?.invoke(currentStepIndex, text)

        if (isTtsInitialized && tts != null) {
            currentUtteranceId = "med_step_${currentStepIndex}_${System.currentTimeMillis()}"
            tts?.setSpeechRate(speechRate)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, currentUtteranceId)
        } else {
            // If TTS is unready, wait interval and proceed
            scheduleNextStep()
        }
    }

    private fun scheduleNextStep() {
        if (isStopped.get() || isPaused.get()) return
        mainHandler.postDelayed({
            if (!isStopped.get() && !isPaused.get()) {
                currentStepIndex++
                speakCurrentStep()
            }
        }, (stepIntervalSeconds * 1000L))
    }

    /**
     * Start Mode B: Audio file guided meditation
     */
    fun startAudioFileSession(uri: Uri) {
        stop()
        audioUri = uri
        isPaused.set(false)
        isStopped.set(false)

        requestAudioFocus()
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(appContext, uri)
                prepareAsync()
                setOnPreparedListener { mp ->
                    mp.start()
                    startProgressTracker()
                }
                setOnCompletionListener {
                    onSessionComplete?.invoke()
                    abandonAudioFocus()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("MeditationAudioHelper", "MediaPlayer error: $what, $extra")
                    onError?.invoke("Audio playback error ($what). Please check file.")
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("MeditationAudioHelper", "Failed to start audio file: ${e.message}")
            onError?.invoke("Could not open selected audio file: ${e.message}")
        }
    }

    private fun startProgressTracker() {
        mainHandler.post(object : Runnable {
            override fun run() {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        try {
                            onAudioProgress?.invoke(mp.currentPosition, mp.duration)
                        } catch (_: Exception) {}
                        mainHandler.postDelayed(this, 1000L)
                    }
                }
            }
        })
    }

    fun pause() {
        isPaused.set(true)
        mainHandler.removeCallbacksAndMessages(null)
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (_: Exception) {}
    }

    fun resume() {
        if (!isPaused.get()) return
        isPaused.set(false)
        requestAudioFocus()

        mediaPlayer?.let { mp ->
            try {
                mp.start()
                startProgressTracker()
                return
            } catch (_: Exception) {}
        }

        // Mode A resume
        speakCurrentStep()
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.let { mp ->
            try {
                mp.seekTo(positionMs)
            } catch (_: Exception) {}
        }
    }

    fun stop() {
        isStopped.set(true)
        isPaused.set(false)
        mainHandler.removeCallbacksAndMessages(null)

        try {
            if (tts?.isSpeaking == true) {
                tts?.stop()
            }
        } catch (_: Exception) {}

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null

        abandonAudioFocus()
    }

    fun release() {
        stop()
        try {
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isTtsInitialized = false
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        pause()
                    }
                }
                .build()
            audioFocusRequest = focusRequest
            audioManager?.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        }
    }

    companion object {
        fun getGuidedSteps(techniqueId: String): List<String> {
            return com.example.ui.screens.meditation.MEDITATION_TECHNIQUES
                .firstOrNull { it.id == techniqueId }
                ?.guidanceSteps
                ?: emptyList()
        }
    }
}
