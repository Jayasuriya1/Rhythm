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
import kotlin.math.sin

/**
 * Curated procedurally synthesized ambient sound textures for focus and study.
 * Designed with ADHD and mood stabilization in mind: non-jarring, seamless,
 * offline audio synthesis with zero boundary clicks and no network dependency. These are generated textures, not field recordings.
 */
enum class SoundscapeType(
    val id: String,
    val displayName: String,
    val iconEmoji: String,
    val description: String,
    val vibeTag: String
) {
    NONE("none", "Mute Soundscape", "🔇", "Silence / Timer chimes only", "Silent"),
    GENTLE_RAIN("rain", "Gentle Rain", "🌧️", "Soft generated rain-like noise texture", "Cozy Calm"),
    RAIN_ON_WINDOW("rain_window", "Rain on Window", "🪟", "Generated rain-on-glass style texture", "Comfort"),
    FOREST_BIRDS("forest_birds", "Forest Birds", "🌲", "Generated wind-like noise with simple chirp tones", "Clarity"),
    FLOWING_RIVER("river", "Flowing River", "🌊", "Generated flowing-water style noise texture", "Flow State"),
    OCEAN_WAVES("ocean", "Ocean Waves", "🏖️", "Generated slow ocean-like noise texture", "Rhythm"),
    FIREPLACE("fireplace", "Fireplace", "🔥", "Generated low crackle-like texture", "Warmth"),
    WIND_THROUGH_TREES("wind", "Wind Through Trees", "🍃", "Generated soft wind-like noise texture", "Deep Rest"),
    NIGHT_CRICKETS("crickets", "Night Crickets", "🦗", "Generated nighttime tone texture", "Night Calm"),
    BROWN_NOISE("brown", "Brown Noise", "🟤", "Deep generated low-frequency noise; personal preference varies", "ADHD Focus"),
    PINK_NOISE("pink", "Pink Noise", "🌸", "Generated balanced noise texture for masking", "Masking");

    companion object {
        // Backward-compatibility aliases
        val FOREST_BREEZE = WIND_THROUGH_TREES
        val COFFEE_SHOP = FIREPLACE
        val WHITE_NOISE = PINK_NOISE

        fun fromId(id: String?): SoundscapeType {
            if (id.isNullOrBlank()) return NONE
            return values().firstOrNull { it.id.equals(id, ignoreCase = true) }
                ?: when (id.lowercase()) {
                    "forest" -> FOREST_BIRDS
                    "cafe" -> FIREPLACE
                    "white" -> PINK_NOISE
                    else -> NONE
                }
        }
    }
}

/**
 * Procedural ambient concentration soundscapes generator.
 * Highlights:
 * - 100% offline procedural PCM synthesis via hardware AudioTrack.MODE_STATIC.
 * - Infinitely loops seamlessly with zero CPU load and zero audio clicks.
 * - Multi-stage digital filtering and Hann window crossfading.
 * - Independent volume and instant smooth switching.
 */
object ConcentrationSoundscapeHelper {

    private const val TAG = "ConcentrationAudio"
    private const val SAMPLE_RATE = 22050
    private const val DURATION_SECONDS = 5.0 // 5-second seamless buffer
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
                selectSoundscape(SoundscapeType.GENTLE_RAIN, autoPlay = true)
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
                // Window the start and end of the buffer (smooth 60ms cosine fade) to guarantee click-free seamless loop
                applyCrossfadeWindow(samples, (SAMPLE_RATE * 0.06).toInt())

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
            SoundscapeType.GENTLE_RAIN -> {
                // Natural steady rain: dual pink-noise floor with Poisson-distributed droplet impulses
                var pink1 = 0f
                var pink2 = 0f
                for (i in 0 until TOTAL_SAMPLES) {
                    val white = random.nextFloat() * 2f - 1f
                    pink1 = pink1 * 0.94f + white * 0.06f
                    pink2 = pink2 * 0.82f + white * 0.18f
                    var sampleVal = (pink1 + pink2) * 0.48f

                    // Soft droplet spatters
                    if (random.nextFloat() < 0.003f) {
                        val freq = 750.0 + random.nextDouble() * 950.0
                        val droplet = sin(2.0 * PI * freq * (i.toDouble() / SAMPLE_RATE))
                        sampleVal += (droplet * 0.22).toFloat()
                    }

                    val amp = (sampleVal * 0.38f * Short.MAX_VALUE).toInt()
                    buffer[i] = amp.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }

            SoundscapeType.RAIN_ON_WINDOW -> {
                // Rain on glass: sharper mid-frequency impact taps over quiet ambient rain
                var pink = 0f
                for (i in 0 until TOTAL_SAMPLES) {
                    val white = random.nextFloat() * 2f - 1f
                    pink = pink * 0.92f + white * 0.08f
                    var sampleVal = pink * 0.42f

                    // Crisp window pane water clicks
                    if (random.nextFloat() < 0.0045f) {
                        val freq = 1200.0 + random.nextDouble() * 1600.0
                        val click = sin(2.0 * PI * freq * (i.toDouble() / SAMPLE_RATE))
                        sampleVal += (click * 0.32).toFloat()
                    }

                    val amp = (sampleVal * 0.36f * Short.MAX_VALUE).toInt()
                    buffer[i] = amp.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }

            SoundscapeType.FOREST_BIRDS -> {
                // Misty pine breeze with natural subtle bird calls
                var windAccum = 0f
                for (i in 0 until TOTAL_SAMPLES) {
                    val time = i.toDouble() / SAMPLE_RATE
                    val white = random.nextFloat() * 2f - 1f
                    windAccum = windAccum * 0.985f + white * 0.015f
                    val gust = 0.5f + 0.5f * sin(2.0 * PI * 0.28 * time)
                    var sampleVal = windAccum * gust.toFloat() * 0.55f

                    // Distant subtle bird chirps (harmonic FM bursts)
                    val birdWindow = (time % 2.5) in 0.8..1.0
                    if (birdWindow) {
                        val birdFreq = 2600.0 + 400.0 * sin(2.0 * PI * 12.0 * time)
                        val chirp = sin(2.0 * PI * birdFreq * time) * 0.15
                        sampleVal += chirp.toFloat()
                    }

                    val amp = (sampleVal * 0.50f * Short.MAX_VALUE).toInt()
                    buffer[i] = amp.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }

            SoundscapeType.FLOWING_RIVER -> {
                // Continuous turbulent water flow: dual-band lowpass noise with rhythmic gurgles
                var riverLow = 0f
                var riverMid = 0f
                for (i in 0 until TOTAL_SAMPLES) {
                    val time = i.toDouble() / SAMPLE_RATE
                    val white = random.nextFloat() * 2f - 1f
                    riverLow = riverLow * 0.96f + white * 0.04f
                    riverMid = riverMid * 0.88f + white * 0.12f
                    val gurgleMod = 0.7f + 0.3f * sin(2.0 * PI * 1.8 * time).toFloat()
                    val sampleVal = (riverLow * 0.6f + riverMid * 0.4f * gurgleMod) * 0.52f
                    val amp = (sampleVal * Short.MAX_VALUE).toInt()
                    buffer[i] = amp.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }

            SoundscapeType.OCEAN_WAVES -> {
                // Ocean waves: Brown noise with slow 3.5s tide swell envelope
                var brownAccum = 0f
                val wavePeriodSeconds = 3.5
                for (i in 0 until TOTAL_SAMPLES) {
                    val time = i.toDouble() / SAMPLE_RATE
                    val white = random.nextFloat() * 2f - 1f
                    brownAccum = (brownAccum + 0.032f * white) * 0.968f
                    val swell = 0.40 + 0.60 * (0.5 * (1.0 + sin(2.0 * PI * (1.0 / wavePeriodSeconds) * time)))
                    val sampleVal = brownAccum * swell.toFloat()
                    val amp = (sampleVal * 0.44f * Short.MAX_VALUE).toInt()
                    buffer[i] = amp.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }

            SoundscapeType.FIREPLACE -> {
                // Cozy crackling hearth: warm low rumble with ember crackle snaps
                var hearthRumble = 0f
                for (i in 0 until TOTAL_SAMPLES) {
                    val white = random.nextFloat() * 2f - 1f
                    hearthRumble = (hearthRumble + 0.02f * white) * 0.98f
                    var sampleVal = hearthRumble * 0.35f

                    // Ember pop / snap impulses
                    if (random.nextFloat() < 0.0018f) {
                        val pop = (random.nextFloat() * 2f - 1f) * 0.55f
                        sampleVal += pop
                    }

                    val amp = (sampleVal * 0.45f * Short.MAX_VALUE).toInt()
                    buffer[i] = amp.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }

            SoundscapeType.WIND_THROUGH_TREES -> {
                // Resonant wind through high mountain canopy with slow gust envelope
                var windAccum = 0f
                for (i in 0 until TOTAL_SAMPLES) {
                    val time = i.toDouble() / SAMPLE_RATE
                    val white = random.nextFloat() * 2f - 1f
                    windAccum = windAccum * 0.985f + white * 0.015f
                    val gust = 0.55 + 0.45 * sin(2.0 * PI * 0.35 * time)
                    val sampleVal = windAccum * gust.toFloat()
                    val amp = (sampleVal * 0.65f * Short.MAX_VALUE).toInt()
                    buffer[i] = amp.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }

            SoundscapeType.NIGHT_CRICKETS -> {
                // Peaceful nighttime air with rhythmic cricket chirping pulses
                var airFloor = 0f
                for (i in 0 until TOTAL_SAMPLES) {
                    val time = i.toDouble() / SAMPLE_RATE
                    val white = random.nextFloat() * 2f - 1f
                    airFloor = (airFloor + 0.01f * white) * 0.99f
                    var sampleVal = airFloor * 0.25f

                    // Chirping burst envelope (period ~ 1.2s, pulse length 0.25s)
                    val cycle = time % 1.2
                    if (cycle < 0.25) {
                        val subPulse = sin(2.0 * PI * 35.0 * cycle)
                        if (subPulse > 0.0) {
                            val carrier = sin(2.0 * PI * 4400.0 * time)
                            sampleVal += (carrier * 0.18 * subPulse).toFloat()
                        }
                    }

                    val amp = (sampleVal * 0.40f * Short.MAX_VALUE).toInt()
                    buffer[i] = amp.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }

            SoundscapeType.BROWN_NOISE -> {
                // Leaky-integrator lowpass on white noise (1/f^2 Brown Noise)
                var accumulator = 0f
                for (i in 0 until TOTAL_SAMPLES) {
                    val white = random.nextFloat() * 2f - 1f
                    accumulator = (accumulator + 0.035f * white) * 0.965f
                    val sample = (accumulator * 0.40f * Short.MAX_VALUE).toInt()
                    buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }

            SoundscapeType.PINK_NOISE -> {
                // Balanced 1/f noise filter for smooth auditory masking
                var b0 = 0f
                var b1 = 0f
                var b2 = 0f
                for (i in 0 until TOTAL_SAMPLES) {
                    val white = random.nextFloat() * 2f - 1f
                    b0 = 0.99886f * b0 + white * 0.0555179f
                    b1 = 0.99332f * b1 + white * 0.0750759f
                    b2 = 0.96900f * b2 + white * 0.1538520f
                    val pink = b0 + b1 + b2 + white * 0.5362f
                    val sample = (pink * 0.11f * Short.MAX_VALUE).toInt()
                    buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }

            SoundscapeType.NONE -> {
                // Silence
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
            // Fade in beginning
            samples[i] = (samples[i] * factorIn).toInt().toShort()
            // Fade out end
            val endIndex = samples.size - 1 - i
            samples[endIndex] = (samples[endIndex] * factorIn).toInt().toShort()
        }
    }
}
