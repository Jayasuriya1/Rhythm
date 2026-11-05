package com.example.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * Procedural ambient concentration soundscapes generator for deep focus and study.
 * Inspired by Flocus and Wonderspace.
 *
 * Highlights:
 * - 100% offline procedural PCM synthesis via hardware AudioTrack.MODE_STATIC.
 * - Zero internet / zero external audio assets required.
 * - Infinitely loops seamlessly with zero CPU load and zero audio popping.
 * - Independent volume and instant smooth switching.
 */
enum class SoundscapeType(
    val id: String,
    val displayName: String,
    val iconEmoji: String,
    val description: String,
    val vibeTag: String
) {
    NONE("none", "Mute Soundscape", "🔇", "Silence / Timer chimes only", "Silent"),
    BROWN_NOISE("brown", "Deep Brown Noise", "🟤", "Heavy rumbling waterfall tone for ADHD & deep flow", "ADHD Focus"),
    GENTLE_RAIN("rain", "Gentle Rain", "🌧️", "Soothing droplets tapping on autumn window leaves", "Cozy Calm"),
    OCEAN_WAVES("ocean", "Midnight Ocean", "🌊", "Rhythmic Pacific tide swells washing on the shore", "Rhythm"),
    FOREST_BREEZE("forest", "Zen Forest Breeze", "🌲", "Misty pine needles whispering in gentle mountain wind", "Clarity"),
    COFFEE_SHOP("cafe", "Cozy Coffee House", "☕", "Warm low-frequency ambient murmur of a rainy cafe", "Warmth"),
    WHITE_NOISE("white", "Pure White Noise", "⚪", "Crisp full-spectrum sound mask to block conversations", "Masking")
}

object ConcentrationSoundscapeHelper {

    private const val TAG = "ConcentrationAudio"
    private const val SAMPLE_RATE = 22050
    private const val DURATION_SECONDS = 4.0 // 4 second loop buffer
    private val TOTAL_SAMPLES = (SAMPLE_RATE * DURATION_SECONDS).toInt()

    private val _currentSoundscape = MutableStateFlow(SoundscapeType.NONE)
    val currentSoundscape: StateFlow<SoundscapeType> = _currentSoundscape.asStateFlow()

    private val _volume = MutableStateFlow(0.65f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var activeTrack: AudioTrack? = null
    private var synthesisJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun selectSoundscape(type: SoundscapeType, autoPlay: Boolean = true) {
        if (_currentSoundscape.value == type && _isPlaying.value) {
            return
        }

        stop()
        _currentSoundscape.value = type

        if (type == SoundscapeType.NONE) {
            return
        }

        if (autoPlay) {
            playCurrent()
        }
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _volume.value = clamped
        try {
            activeTrack?.setVolume(clamped)
        } catch (e: Exception) {
            Log.w(TAG, "Error adjusting soundscape volume: ${e.message}")
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            if (_currentSoundscape.value == SoundscapeType.NONE) {
                selectSoundscape(SoundscapeType.BROWN_NOISE, autoPlay = true)
            } else {
                playCurrent()
            }
        }
    }

    fun playCurrent() {
        val type = _currentSoundscape.value
        if (type == SoundscapeType.NONE) return

        synthesisJob?.cancel()
        synthesisJob = scope.launch {
            try {
                val samples = generateSamplesForType(type)
                // Window the start and end of the buffer (smooth 50ms cosine fade) to guarantee click-free seamless loop
                applyCrossfadeWindow(samples, (SAMPLE_RATE * 0.05).toInt())

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
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(samples, 0, samples.size)
                track.setLoopPoints(0, samples.size, -1) // Infinite hardware loop
                track.setVolume(_volume.value)
                track.play()

                activeTrack?.release()
                activeTrack = track
                _isPlaying.value = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start soundscape $type: ${e.message}")
                _isPlaying.value = false
            }
        }
    }

    fun pause() {
        try {
            activeTrack?.pause()
        } catch (_: Exception) {}
        _isPlaying.value = false
    }

    fun stop() {
        synthesisJob?.cancel()
        try {
            activeTrack?.stop()
            activeTrack?.release()
        } catch (_: Exception) {}
        activeTrack = null
        _isPlaying.value = false
    }

    private fun generateSamplesForType(type: SoundscapeType): ShortArray {
        val buffer = ShortArray(TOTAL_SAMPLES)
        val random = Random(42)

        when (type) {
            SoundscapeType.BROWN_NOISE -> {
                // Leaky integrator low-pass filter on white noise gives 1/f^2 Brown Noise
                var accumulator = 0f
                for (i in 0 until TOTAL_SAMPLES) {
                    val white = (random.nextFloat() * 2f - 1f)
                    accumulator = (accumulator + 0.035f * white) * 0.965f
                    val sample = (accumulator * 0.38f * Short.MAX_VALUE).toInt()
                    buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }

            SoundscapeType.GENTLE_RAIN -> {
                // Rain = Pink noise base + Poisson random water droplet impulse resonances
                var pinkAccum1 = 0f
                var pinkAccum2 = 0f
                for (i in 0 until TOTAL_SAMPLES) {
                    val white = (random.nextFloat() * 2f - 1f)
                    pinkAccum1 = pinkAccum1 * 0.95f + white * 0.05f
                    pinkAccum2 = pinkAccum2 * 0.85f + white * 0.15f
                    var sampleVal = (pinkAccum1 + pinkAccum2) * 0.5f

                    // Random droplet splatters
                    if (random.nextFloat() < 0.0035f) {
                        val dropletFreq = 800.0 + random.nextDouble() * 1200.0
                        val dropletPhase = sin(2.0 * PI * dropletFreq * (i.toDouble() / SAMPLE_RATE))
                        sampleVal += (dropletPhase * 0.28).toFloat()
                    }

                    val amplitude = (sampleVal * 0.35f * Short.MAX_VALUE).toInt()
                    buffer[i] = amplitude.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }

            SoundscapeType.OCEAN_WAVES -> {
                // Waves = Brown/Pink noise with slow sinusoidal surge modulation (~0.25 Hz surge)
                var brownAccum = 0f
                val wavePeriodSeconds = 3.5
                for (i in 0 until TOTAL_SAMPLES) {
                    val time = i.toDouble() / SAMPLE_RATE
                    val white = (random.nextFloat() * 2f - 1f)
                    brownAccum = (brownAccum + 0.03f * white) * 0.97f

                    // Wave swell envelope
                    val swell = 0.45 + 0.55 * (0.5 * (1.0 + sin(2.0 * PI * (1.0 / wavePeriodSeconds) * time)))
                    val sampleVal = brownAccum * swell.toFloat()
                    val amplitude = (sampleVal * 0.42f * Short.MAX_VALUE).toInt()
                    buffer[i] = amplitude.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }

            SoundscapeType.FOREST_BREEZE -> {
                // Breeze = Resonant filtered wind with slow gust envelope
                var windAccum = 0f
                for (i in 0 until TOTAL_SAMPLES) {
                    val time = i.toDouble() / SAMPLE_RATE
                    val white = (random.nextFloat() * 2f - 1f)
                    windAccum = windAccum * 0.985f + white * 0.015f

                    val gust = 0.55 + 0.45 * sin(2.0 * PI * 0.35 * time)
                    val sampleVal = windAccum * gust.toFloat()
                    val amplitude = (sampleVal * 0.65f * Short.MAX_VALUE).toInt()
                    buffer[i] = amplitude.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }

            SoundscapeType.COFFEE_SHOP -> {
                // Warm ambient cafe hum: gentle low-mid band murmur
                var cafeAccum = 0f
                for (i in 0 until TOTAL_SAMPLES) {
                    val white = (random.nextFloat() * 2f - 1f)
                    cafeAccum = cafeAccum * 0.92f + white * 0.08f
                    var sampleVal = cafeAccum * 0.65f

                    // Gentle subtle warm clinks
                    if (random.nextFloat() < 0.0006f) {
                        val clink = sin(2.0 * PI * 2200.0 * (i.toDouble() / SAMPLE_RATE))
                        sampleVal += (clink * 0.12).toFloat()
                    }

                    val amplitude = (sampleVal * 0.38f * Short.MAX_VALUE).toInt()
                    buffer[i] = amplitude.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }

            SoundscapeType.WHITE_NOISE -> {
                // Pure uniform flat white noise
                for (i in 0 until TOTAL_SAMPLES) {
                    val white = (random.nextFloat() * 2f - 1f)
                    val amplitude = (white * 0.20f * Short.MAX_VALUE).toInt()
                    buffer[i] = amplitude.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }

            SoundscapeType.NONE -> {
                // Silent
            }
        }

        return buffer
    }

    private fun applyCrossfadeWindow(samples: ShortArray, windowSamples: Int) {
        val window = minOf(windowSamples, samples.size / 4)
        for (i in 0 until window) {
            val progress = i.toDouble() / window
            // Hann window curve
            val factorIn = 0.5 * (1.0 - cos(PI * progress))
            val factorOut = 0.5 * (1.0 + cos(PI * progress))

            // Fade in beginning
            samples[i] = (samples[i] * factorIn).toInt().toShort()

            // Fade out end
            val endIndex = samples.size - 1 - i
            samples[endIndex] = (samples[endIndex] * factorIn).toInt().toShort()
        }
    }
}
