package com.example.util

import android.content.Context
import android.content.Intent
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
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Repeat modes for offline music player.
 */
enum class MusicRepeatMode {
    OFF,
    REPEAT_ONE,
    REPEAT_ALL
}

typealias RepeatMode = MusicRepeatMode

/**
 * Representation of a track in the offline focus music playlist.
 */
data class MusicTrack(
    val id: String = UUID.randomUUID().toString(),
    val uriString: String,
    val title: String,
    val artist: String? = null,
    val durationMs: Int = 0
)

/**
 * Full-featured offline music player for Focus Studio.
 * Supports:
 * - Multi-track focus playlist management (add, remove, clear, move)
 * - Play, Pause, Next, Previous, Seek ±10s, Progress scrubber
 * - Shuffle & Repeat modes (Repeat One, Repeat All, Off)
 * - Independent volume control & auto-sync with study timer
 * - Resilient handling for missing or inaccessible local URIs
 */
object StudyMusicPlayer {

    private const val TAG = "StudyMusicPlayer"

    // Playlist State
    private val _playlist = MutableStateFlow<List<MusicTrack>>(emptyList())
    val playlist: StateFlow<List<MusicTrack>> = _playlist.asStateFlow()

    private val _currentTrackIndex = MutableStateFlow(-1)
    val currentTrackIndex: StateFlow<Int> = _currentTrackIndex.asStateFlow()

    private val _currentTrack = MutableStateFlow<MusicTrack?>(null)
    val currentTrack: StateFlow<MusicTrack?> = _currentTrack.asStateFlow()

    // Backward-compatibility flows for existing UI references
    val currentTrackTitle: StateFlow<String?> = MutableStateFlow<String?>("No Song Selected").apply {
        // Updated whenever currentTrack changes
    }
    private val _currentTrackTitle = currentTrackTitle as MutableStateFlow<String?>

    val currentTrackUri: StateFlow<Uri?> = MutableStateFlow<Uri?>(null)
    private val _currentTrackUri = currentTrackUri as MutableStateFlow<Uri?>

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs: StateFlow<Int> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0)
    val durationMs: StateFlow<Int> = _durationMs.asStateFlow()

    private val _volume = MutableStateFlow(0.70f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _repeatMode = MutableStateFlow(MusicRepeatMode.REPEAT_ALL)
    val repeatMode: StateFlow<MusicRepeatMode> = _repeatMode.asStateFlow()

    // Backward compatibility for isLooping
    val isLooping: StateFlow<Boolean> = MutableStateFlow(true)
    private val _isLooping = isLooping as MutableStateFlow<Boolean>

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _autoPlayWithTimer = MutableStateFlow(true)
    val autoPlayWithTimer: StateFlow<Boolean> = _autoPlayWithTimer.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressTrackerJob: Job? = null
    private val playerScope = CoroutineScope(Dispatchers.Main)
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Add multiple tracks to the playlist from device Storage Access Framework URIs.
     */
    fun addTracks(context: Context, uris: List<Uri>) {
        appContext = context.applicationContext
        val newTracks = mutableListOf<MusicTrack>()

        for (uri in uris) {
            try {
                // Persist read permissions so the audio file remains playable across restarts
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (_: Exception) {
                // Provider may not support persistable permissions; file is still accessible in current session
            }

            val title = resolveFileName(context, uri) ?: "Track ${(_playlist.value.size + newTracks.size + 1)}"
            newTracks.add(
                MusicTrack(
                    uriString = uri.toString(),
                    title = title
                )
            )
        }

        if (newTracks.isNotEmpty()) {
            val updated = _playlist.value + newTracks
            _playlist.value = updated

            // If no track was loaded, select the first track
            if (_currentTrackIndex.value == -1 && updated.isNotEmpty()) {
                selectTrack(context, 0, autoPlay = false)
            }
        }
    }

    /**
     * Single track loader for backward compatibility.
     */
    fun loadLocalTrack(context: Context, uri: Uri) {
        addTracks(context, listOf(uri))
        if (_playlist.value.isNotEmpty()) {
            selectTrack(context, _playlist.value.lastIndex, autoPlay = false)
        }
    }

    fun selectTrack(context: Context, index: Int, autoPlay: Boolean = false) {
        val list = _playlist.value
        if (index !in list.indices) return

        appContext = context.applicationContext
        _currentTrackIndex.value = index
        val track = list[index]
        _currentTrack.value = track
        _currentTrackTitle.value = track.title
        _currentTrackUri.value = Uri.parse(track.uriString)

        releasePlayer()
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, Uri.parse(track.uriString))
                isLooping = (_repeatMode.value == MusicRepeatMode.REPEAT_ONE)
                setVolume(_volume.value, _volume.value)
                setOnCompletionListener {
                    handleTrackCompletion()
                }
                prepare()
                _durationMs.value = duration
                _currentPositionMs.value = 0
            }

            if (autoPlay) {
                play()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load track ${track.title}: ${e.message}", e)
            _currentTrackTitle.value = "Error: ${track.title}"
            _isPlaying.value = false
        }
    }

    fun play() {
        val player = mediaPlayer ?: run {
            val ctx = appContext
            if (ctx != null && _playlist.value.isNotEmpty() && _currentTrackIndex.value >= 0) {
                selectTrack(ctx, _currentTrackIndex.value, autoPlay = true)
                return
            }
            return
        }

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
        } catch (_: Exception) {}
        _isPlaying.value = false
        progressTrackerJob?.cancel()
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun nextTrack() {
        val list = _playlist.value
        if (list.isEmpty()) return

        val nextIndex = if (_isShuffle.value) {
            val available = list.indices.filter { it != _currentTrackIndex.value }
            if (available.isNotEmpty()) available.random() else 0
        } else {
            (_currentTrackIndex.value + 1) % list.size
        }

        val ctx = appContext
        if (ctx != null) {
            selectTrack(ctx, nextIndex, autoPlay = _isPlaying.value)
        }
    }

    fun previousTrack() {
        val list = _playlist.value
        if (list.isEmpty()) return

        // If played more than 3 seconds, rewind to start of current track
        if (_currentPositionMs.value > 3000) {
            seekTo(0)
            return
        }

        val prevIndex = if (_currentTrackIndex.value - 1 < 0) list.size - 1 else _currentTrackIndex.value - 1
        val ctx = appContext
        if (ctx != null) {
            selectTrack(ctx, prevIndex, autoPlay = _isPlaying.value)
        }
    }

    fun seekTo(positionMs: Int) {
        val player = mediaPlayer ?: return
        try {
            val target = positionMs.coerceIn(0, _durationMs.value)
            player.seekTo(target)
            _currentPositionMs.value = target
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking audio: ${e.message}")
        }
    }

    fun seekRelative(deltaMs: Int) {
        seekTo(_currentPositionMs.value + deltaMs)
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _volume.value = clamped
        try {
            mediaPlayer?.setVolume(clamped, clamped)
        } catch (_: Exception) {}
    }

    fun setRepeatMode(mode: MusicRepeatMode) {
        _repeatMode.value = mode
        _isLooping.value = (mode == MusicRepeatMode.REPEAT_ONE)
        try {
            mediaPlayer?.isLooping = (mode == MusicRepeatMode.REPEAT_ONE)
        } catch (_: Exception) {}
    }

    fun toggleRepeatMode() {
        val next = when (_repeatMode.value) {
            MusicRepeatMode.OFF -> MusicRepeatMode.REPEAT_ALL
            MusicRepeatMode.REPEAT_ALL -> MusicRepeatMode.REPEAT_ONE
            MusicRepeatMode.REPEAT_ONE -> MusicRepeatMode.OFF
        }
        setRepeatMode(next)
    }

    fun setLooping(loop: Boolean) {
        setRepeatMode(if (loop) MusicRepeatMode.REPEAT_ONE else MusicRepeatMode.REPEAT_ALL)
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun setAutoPlayWithTimer(enabled: Boolean) {
        _autoPlayWithTimer.value = enabled
    }

    fun removeTrack(index: Int) {
        val list = _playlist.value.toMutableList()
        if (index !in list.indices) return

        val wasCurrent = (index == _currentTrackIndex.value)
        list.removeAt(index)
        _playlist.value = list

        if (list.isEmpty()) {
            clearTrack()
        } else if (wasCurrent) {
            val nextIdx = minOf(index, list.size - 1)
            val ctx = appContext
            if (ctx != null) {
                selectTrack(ctx, nextIdx, autoPlay = _isPlaying.value)
            }
        } else if (index < _currentTrackIndex.value) {
            _currentTrackIndex.value = _currentTrackIndex.value - 1
        }
    }

    fun clearTrack() {
        releasePlayer()
        _playlist.value = emptyList()
        _currentTrackIndex.value = -1
        _currentTrack.value = null
        _currentTrackUri.value = null
        _currentTrackTitle.value = "No Song Selected"
        _isPlaying.value = false
        _currentPositionMs.value = 0
        _durationMs.value = 0
    }

    val isShuffleEnabled: StateFlow<Boolean> get() = _isShuffle

    fun skipNext() = nextTrack()
    fun skipPrevious() = previousTrack()
    fun cycleRepeatMode() = toggleRepeatMode()
    fun clearPlaylist() = clearTrack()

    fun setPlaylistForTesting(tracks: List<MusicTrack>) {
        _playlist.value = tracks
        if (tracks.isNotEmpty()) {
            _currentTrackIndex.value = 0
            _currentTrack.value = tracks[0]
            _currentTrackTitle.value = tracks[0].title
        } else {
            clearTrack()
        }
    }

    fun removeTrack(id: String) {
        val idx = _playlist.value.indexOfFirst { it.id == id }
        if (idx != -1) {
            removeTrack(idx)
        }
    }

    /**
     * Timer Lifecycle Callbacks
     */
    fun onStudyTimerStarted() {
        if (_autoPlayWithTimer.value && _playlist.value.isNotEmpty()) {
            play()
        }
    }

    fun onStudyTimerPaused() {
        if (_isPlaying.value) {
            pause()
        }
    }

    fun onStudyTimerStopped() {
        if (_isPlaying.value) {
            pause()
        }
    }

    private fun handleTrackCompletion() {
        when (_repeatMode.value) {
            MusicRepeatMode.REPEAT_ONE -> {
                seekTo(0)
                play()
            }
            MusicRepeatMode.REPEAT_ALL -> {
                nextTrack()
            }
            MusicRepeatMode.OFF -> {
                if (_currentTrackIndex.value < _playlist.value.size - 1) {
                    nextTrack()
                } else {
                    pause()
                    seekTo(0)
                }
            }
        }
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
