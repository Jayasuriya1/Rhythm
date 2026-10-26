package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object StudySoundHelper {

    private const val SAMPLE_RATE = 44100
    private var activeTrack: AudioTrack? = null

    /**
     * Synthesizes and plays a gentle, warm rising chime (C5 523 Hz -> E5 659 Hz)
     * signaling the start of a deep focus session.
     */
    fun playStartSound(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val focusReq = requestFocus(audioManager)

            try {
                // Generate ~400ms gentle two-tone rising chime
                val durationSec = 0.42
                val numSamples = (SAMPLE_RATE * durationSec).toInt()
                val buffer = ShortArray(numSamples)

                val tone1Samples = (SAMPLE_RATE * 0.16).toInt()
                val freq1 = 523.25 // C5
                val freq2 = 659.25 // E5

                for (i in 0 until numSamples) {
                    val time = i.toDouble() / SAMPLE_RATE
                    val sample = if (i < tone1Samples) {
                        val env = exp(-i.toDouble() / (SAMPLE_RATE * 0.10))
                        sin(2.0 * PI * freq1 * time) * env
                    } else {
                        val subIndex = i - tone1Samples
                        val env = exp(-subIndex.toDouble() / (SAMPLE_RATE * 0.22))
                        sin(2.0 * PI * freq2 * time) * env
                    }
                    val amplitude = (sample * 0.35 * Short.MAX_VALUE).toInt()
                    buffer[i] = amplitude.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                playBuffer(buffer)
            } finally {
                abandonFocus(audioManager, focusReq)
            }
        }
    }

    /**
     * Synthesizes and plays a resonant, calming singing bowl chime (A4 440 Hz + harmonic 880 Hz)
     * signaling the completion of a study timer.
     */
    fun playEndSound(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val focusReq = requestFocus(audioManager)

            try {
                // Generate ~850ms calming Tibetan singing bowl tone with gentle exponential decay
                val durationSec = 0.85
                val numSamples = (SAMPLE_RATE * durationSec).toInt()
                val buffer = ShortArray(numSamples)

                val fundamental = 440.0 // A4
                val harmonic = 880.0    // A5

                for (i in 0 until numSamples) {
                    val time = i.toDouble() / SAMPLE_RATE
                    val decay = exp(-time * 3.6)
                    val s1 = sin(2.0 * PI * fundamental * time) * 0.75
                    val s2 = sin(2.0 * PI * harmonic * time) * 0.25
                    val sample = (s1 + s2) * decay
                    val amplitude = (sample * 0.38 * Short.MAX_VALUE).toInt()
                    buffer[i] = amplitude.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                playBuffer(buffer)
            } finally {
                abandonFocus(audioManager, focusReq)
            }
        }
    }

    private suspend fun playBuffer(buffer: ShortArray) {
        try {
            stopCurrent()
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            activeTrack = track
            track.write(buffer, 0, buffer.size)
            track.play()

            val durationMillis = (buffer.size.toDouble() / SAMPLE_RATE * 1000).toLong() + 100
            delay(durationMillis)
        } catch (_: Exception) {
        } finally {
            stopCurrent()
        }
    }

    private fun stopCurrent() {
        try {
            activeTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
        } catch (_: Exception) {
        } finally {
            activeTrack = null
        }
    }

    private fun requestFocus(audioManager: AudioManager?): Any? {
        if (audioManager == null) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .build()
            audioManager.requestAudioFocus(req)
            req
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_NOTIFICATION, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            null
        }
    }

    private fun abandonFocus(audioManager: AudioManager?, focusReq: Any?) {
        if (audioManager == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusReq is AudioFocusRequest) {
            audioManager.abandonAudioFocusRequest(focusReq)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }
}
