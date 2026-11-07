package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log

/**
 * Authoritative Focus Audio Lifecycle Controller.
 * Guarantees that soundscapes and timer-linked music strictly obey the study timer
 * and Android audio lifecycle without ghost playback.
 *
 * Implements:
 * - Immediate, idempotent pause on timer pause, reset, or completion
 * - Android AudioFocus gain, ducking, and loss handling via AudioManager
 * - Noisy broadcast receiver: pauses playback when headphones/Bluetooth disconnect
 * - Navigation & backgrounding enforcement: pauses audio when leaving Study unless
 *   the user has explicitly opted into "Audio while studying in other apps".
 */
object FocusAudioLifecycleManager {

    private const val TAG = "FocusAudioController"

    private var appContext: Context? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var isRegisteredForNoisy = false

    // Flag indicating whether audio was playing prior to a transient interruption
    private var wasPlayingBeforeTransientLoss = false

    // User preference: default false (Off by default as required by specification)
    private var backgroundAudioAllowed = false

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                Log.d(TAG, "Audio becoming noisy (headphones disconnected) -> pausing focus audio")
                pauseAllFocusAudio()
            }
        }
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "AudioFocus permanent loss -> stopping focus audio")
                wasPlayingBeforeTransientLoss = false
                pauseAllFocusAudio()
                abandonAudioFocus()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(TAG, "AudioFocus transient loss -> pausing focus audio")
                wasPlayingBeforeTransientLoss = isAnyAudioPlaying()
                pauseAllFocusAudio()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "AudioFocus transient duck -> ducking volume")
                // Lower volume to 25% temporarily
                ConcentrationSoundscapeHelper.setVolume(ConcentrationSoundscapeHelper.volume.value * 0.25f)
                StudyMusicPlayer.setVolume(StudyMusicPlayer.volume.value * 0.25f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "AudioFocus gained -> restoring volume and playback")
                if (wasPlayingBeforeTransientLoss) {
                    wasPlayingBeforeTransientLoss = false
                    resumeAllFocusAudio()
                }
            }
        }
    }

    fun initialize(context: Context) {
        val app = context.applicationContext
        appContext = app
        audioManager = app.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        StudyMusicPlayer.initialize(app)
        registerNoisyReceiver()
    }

    fun setBackgroundAudioAllowed(allowed: Boolean) {
        backgroundAudioAllowed = allowed
    }

    fun isBackgroundAudioAllowed(): Boolean = backgroundAudioAllowed

    // -------------------------------------------------------------
    // Timer Event Mappings
    // -------------------------------------------------------------

    /**
     * Start Focus / Resume Focus: Starts configured soundscape and/or timer-linked music.
     */
    fun onFocusTimerStarted(activeSoundscape: SoundscapeType = ConcentrationSoundscapeHelper.currentSoundscape.value) {
        requestAudioFocus()
        registerNoisyReceiver()

        // 1. Soundscape
        if (activeSoundscape != SoundscapeType.NONE && !ConcentrationSoundscapeHelper.isPlaying.value) {
            ConcentrationSoundscapeHelper.playCurrent()
        }

        // 2. Music Player
        StudyMusicPlayer.onStudyTimerStarted()
    }

    /**
     * Pause Focus: Immediately and idempotently pauses both soundscape and music.
     */
    fun onFocusTimerPaused() {
        pauseAllFocusAudio()
    }

    /**
     * Stop / Reset / End Focus: Stops audio immediately and releases focus ownership.
     */
    fun onFocusTimerStopped() {
        stopAllFocusAudio()
        abandonAudioFocus()
    }

    /**
     * Focus Session Completes: Stops ongoing audio, then allows completion chime.
     */
    fun onFocusSessionCompleted() {
        stopAllFocusAudio()
        abandonAudioFocus()
    }

    /**
     * Navigated Away from Study Tab: Pauses audio unless user opted into background audio.
     */
    fun onNavigatedAway(isTimerRunning: Boolean) {
        if (!backgroundAudioAllowed || !isTimerRunning) {
            pauseAllFocusAudio()
        }
    }

    /**
     * Returned to Study Tab: Never auto-plays if timer is paused.
     */
    fun onReturnedToStudy(isTimerRunning: Boolean) {
        if (isTimerRunning) {
            resumeAllFocusAudio()
        }
    }

    /**
     * App Backgrounded / Minimized: Enforces background audio setting.
     */
    fun onAppBackgrounded(isTimerRunning: Boolean) {
        if (!backgroundAudioAllowed || !isTimerRunning) {
            pauseAllFocusAudio()
        }
    }

    /**
     * App Foregrounded: Restores playback only if timer was running.
     */
    fun onAppForegrounded(isTimerRunning: Boolean) {
        if (isTimerRunning && (ConcentrationSoundscapeHelper.currentSoundscape.value != SoundscapeType.NONE || StudyMusicPlayer.playlist.value.isNotEmpty())) {
            resumeAllFocusAudio()
        }
    }

    // -------------------------------------------------------------
    // Audio Helper Actions
    // -------------------------------------------------------------

    fun pauseAllFocusAudio() {
        try {
            ConcentrationSoundscapeHelper.pause()
            StudyMusicPlayer.pause()
        } catch (e: Exception) {
            Log.w(TAG, "Error pausing focus audio: ${e.message}")
        }
    }

    fun stopAllFocusAudio() {
        try {
            ConcentrationSoundscapeHelper.stop()
            StudyMusicPlayer.pause()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping focus audio: ${e.message}")
        }
    }

    fun resumeAllFocusAudio() {
        requestAudioFocus()
        if (ConcentrationSoundscapeHelper.currentSoundscape.value != SoundscapeType.NONE) {
            ConcentrationSoundscapeHelper.playCurrent()
        }
        if (StudyMusicPlayer.playlist.value.isNotEmpty()) {
            StudyMusicPlayer.play()
        }
    }

    fun isAnyAudioPlaying(): Boolean {
        return ConcentrationSoundscapeHelper.isPlaying.value || StudyMusicPlayer.isPlaying.value
    }

    private fun requestAudioFocus(): Boolean {
        val am = audioManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = req
            am.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    private fun registerNoisyReceiver() {
        val context = appContext ?: return
        if (!isRegisteredForNoisy) {
            try {
                context.registerReceiver(
                    noisyReceiver,
                    IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                )
                isRegisteredForNoisy = true
            } catch (e: Exception) {
                Log.w(TAG, "Failed to register noisy receiver: ${e.message}")
            }
        }
    }

    fun cleanup() {
        val context = appContext ?: return
        if (isRegisteredForNoisy) {
            try {
                context.unregisterReceiver(noisyReceiver)
            } catch (_: Exception) {}
            isRegisteredForNoisy = false
        }
        abandonAudioFocus()
        stopAllFocusAudio()
    }
}
