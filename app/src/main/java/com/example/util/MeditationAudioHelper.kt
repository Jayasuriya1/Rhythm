package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class MeditationAudioMode(val displayName: String, val description: String) {
    TTS("Spoken Instructions", "Mindful voice prompts pacing the exercise"),
    BREATHING_CUES("Breathing Sound Cues", "Gentle acoustic tones for Inhale, Hold, and Exhale"),
    AMBIENT("Ambient Audio", "Calming background drone or local audio track"),
    SILENT("Silent Visual Pacing", "Quiet session guided purely by visual breathing circle")
}

data class BreathingPacing(
    val inhaleSeconds: Int = 4,
    val holdInhaleSeconds: Int = 4,
    val exhaleSeconds: Int = 4,
    val holdExhaleSeconds: Int = 0
) {
    val totalCycleSeconds: Int
        get() = inhaleSeconds + holdInhaleSeconds + exhaleSeconds + holdExhaleSeconds

    companion object {
        val BOX_BREATHING = BreathingPacing(4, 4, 4, 4)
        val RELAXING_4_7_8 = BreathingPacing(4, 7, 8, 0)
        val DEEP_CALM = BreathingPacing(5, 2, 6, 0)
        val NATURAL_FLOW = BreathingPacing(3, 0, 3, 0)
    }
}

/**
 * Controller for Voice and Audio Guidance in Meditation.
 * Supports:
 * - Mode 1: Text-to-Speech (TTS) calm instruction pacing, adjustable rate, voice preview.
 * - Mode 2: Subtle PCM synthesized sound cues for breathing phases (Inhale, Hold, Exhale).
 * - Mode 3: Ambient background audio drone or local user-selected track.
 * - Mode 4: Silent visual pacing.
 * - Complete Audio Focus polite ducking and clean resource cancellation.
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
    private var speechRate: Float = 0.85f
    private var stepIntervalSeconds: Int = 12

    // Audio file player state
    private var mediaPlayer: MediaPlayer? = null
    private var ambientTrack: AudioTrack? = null
    private var audioUri: Uri? = null

    // Audio focus
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    // PCM Cue Track
    private var cueTrack: AudioTrack? = null
    private val sampleRate = 44100

    // Callbacks
    var onStepSpoken: ((Int, String) -> Unit)? = null
    var onSessionComplete: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onAudioProgress: ((Int, Int) -> Unit)? = null

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
     * Start Mode 1: TTS Guided Meditation
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
     * Mode 2: Breathing Sound Cues (Synthesized PCM Tones)
     */
    fun playInhaleCue() {
        if (isPaused.get() || isStopped.get()) return
        playSynthesizedTone(startFreq = 300.0, endFreq = 460.0, durationSec = 0.35, amplitudeScale = 0.28)
    }

    fun playHoldCue() {
        if (isPaused.get() || isStopped.get()) return
        playSynthesizedTone(startFreq = 528.0, endFreq = 528.0, durationSec = 0.22, amplitudeScale = 0.22)
    }

    fun playExhaleCue() {
        if (isPaused.get() || isStopped.get()) return
        playSynthesizedTone(startFreq = 460.0, endFreq = 300.0, durationSec = 0.35, amplitudeScale = 0.28)
    }

    private fun playSynthesizedTone(startFreq: Double, endFreq: Double, durationSec: Double, amplitudeScale: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                stopCueTrack()
                val numSamples = (sampleRate * durationSec).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    val freq = startFreq + (endFreq - startFreq) * progress
                    val time = i.toDouble() / sampleRate
                    val envelope = sin(PI * progress) // smooth parabolic rise & fall
                    val sample = sin(2.0 * PI * freq * time) * envelope
                    buffer[i] = (sample * amplitudeScale * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                cueTrack = track
                track.write(buffer, 0, buffer.size)
                track.play()
            } catch (_: Exception) {}
        }
    }

    private fun stopCueTrack() {
        try {
            cueTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
        } catch (_: Exception) {} finally {
            cueTrack = null
        }
    }

    /**
     * Mode 3: Ambient Audio (Prerecorded file or gentle synthetic binaural drone)
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
                isLooping = true
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

    fun startSyntheticAmbientDrone() {
        stop()
        isPaused.set(false)
        isStopped.set(false)
        requestAudioFocus()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 3-second loopable warm ambient harmonic chord (108Hz + 432Hz fifth)
                val durationSec = 3.0
                val numSamples = (sampleRate * durationSec).toInt()
                val buffer = ShortArray(numSamples)
                val freq1 = 108.0
                val freq2 = 432.0

                for (i in 0 until numSamples) {
                    val time = i.toDouble() / sampleRate
                    val s1 = sin(2.0 * PI * freq1 * time) * 0.7
                    val s2 = sin(2.0 * PI * freq2 * time) * 0.3
                    val sample = (s1 + s2) * 0.18
                    buffer[i] = (sample * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.setLoopPoints(0, buffer.size, -1)
                ambientTrack = track
                track.play()
            } catch (_: Exception) {}
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
            if (ambientTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                ambientTrack?.pause()
            }
            stopCueTrack()
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

        ambientTrack?.let {
            try {
                it.play()
                return
            } catch (_: Exception) {}
        }

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

        try {
            ambientTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
        } catch (_: Exception) {}
        ambientTrack = null

        stopCueTrack()
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
