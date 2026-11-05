package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Manager for user-selected local audio tracks and songs to play during study focus sessions.
 * Supports:
 * - Loading local audio file via SAF Uri (MP3, M4A, FLAC, WAV, OGG)
 * - Track title and filename resolution
 * - Play / Pause / Seek / Volume control
 * - Continuous looping
 * - Synchronized auto-play and pause when study focus timer starts and pauses
 */
object StudyMusicPlayer {

    private const val TAG = "StudyMusicPlayer"

    private val _currentTrackUri = MutableStateFlow<Uri?>(null)
    val currentTrackUri: StateFlow<Uri?> = _currentTrackUri.asStateFlow()

    private val _currentTrackTitle = MutableStateFlow<String?>("No Song Selected")
    val currentTrackTitle: StateFlow<String?> = _currentTrackTitle.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs: StateFlow<Int> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0)
    val durationMs: StateFlow<Int> = _durationMs.asStateFlow()

    private val _volume = MutableStateFlow(0.70f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _isLooping = MutableStateFlow(true)
    val isLooping: StateFlow<Boolean> = _isLooping.asStateFlow()

    private val _autoPlayWithTimer = MutableStateFlow(true)
    val autoPlayWithTimer: StateFlow<Boolean> = _autoPlayWithTimer.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressTrackerJob: Job? = null
    private val playerScope = CoroutineScope(Dispatchers.Main)

    fun loadLocalTrack(context: Context, uri: Uri) {
        val title = resolveFileName(context, uri) ?: "Selected Song"
        _currentTrackUri.value = uri
        _currentTrackTitle.value = title

        // Initialize media player
        releasePlayer()
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, uri)
                isLooping = _isLooping.value
                setVolume(_volume.value, _volume.value)
                prepare()
                _durationMs.value = duration
                _currentPositionMs.value = 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load local track: ${e.message}", e)
            _currentTrackTitle.value = "Error loading: $title"
        }
    }

    fun play() {
        val player = mediaPlayer ?: return
        try {
            if (!player.isPlaying) {
                player.start()
                _isPlaying.value = true
                startProgressTracker()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio: ${e.message}")
        }
    }

    fun pause() {
        val player = mediaPlayer ?: return
        try {
            if (player.isPlaying) {
                player.pause()
            }
            _isPlaying.value = false
            progressTrackerJob?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing audio: ${e.message}")
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Int) {
        val player = mediaPlayer ?: return
        try {
            player.seekTo(positionMs)
            _currentPositionMs.value = positionMs
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking audio: ${e.message}")
        }
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _volume.value = clamped
        try {
            mediaPlayer?.setVolume(clamped, clamped)
        } catch (_: Exception) {}
    }

    fun setLooping(loop: Boolean) {
        _isLooping.value = loop
        try {
            mediaPlayer?.isLooping = loop
        } catch (_: Exception) {}
    }

    fun setAutoPlayWithTimer(enabled: Boolean) {
        _autoPlayWithTimer.value = enabled
    }

    /**
     * Called when the Study Timer starts or resumes.
     */
    fun onStudyTimerStarted() {
        if (_autoPlayWithTimer.value && _currentTrackUri.value != null && mediaPlayer != null) {
            play()
        }
    }

    /**
     * Called when the Study Timer is paused or stopped.
     */
    fun onStudyTimerPaused() {
        if (_autoPlayWithTimer.value && _isPlaying.value) {
            pause()
        }
    }

    fun clearTrack() {
        releasePlayer()
        _currentTrackUri.value = null
        _currentTrackTitle.value = "No Song Selected"
        _isPlaying.value = false
        _currentPositionMs.value = 0
        _durationMs.value = 0
    }

    private fun startProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = playerScope.launch {
            while (isActive && _isPlaying.value) {
                try {
                    mediaPlayer?.let { player ->
                        if (player.isPlaying) {
                            _currentPositionMs.value = player.currentPosition
                        }
                    }
                } catch (_: Exception) {}
                delay(500)
            }
        }
    }

    private fun releasePlayer() {
        progressTrackerJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    private fun resolveFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (_: Exception) {}
        return name ?: uri.lastPathSegment
    }
}
