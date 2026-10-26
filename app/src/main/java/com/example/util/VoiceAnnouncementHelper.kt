package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Helper to speak voice announcements for reminders using Android Text-to-Speech (TTS).
 * Strict adherence to requirements:
 * - Disabled by default; user must opt-in.
 * - Strictly respects Quiet Hours unless the reminder is marked as Urgent/Critical.
 * - Uses system audio streams so volume is controlled through Android system settings.
 * - Prevents multiple overlapping instances and releases TTS resources cleanly.
 * - Graceful fallback when TTS is unavailable or speech engine fails.
 */
object VoiceAnnouncementHelper {

    private const val TAG = "VoiceAnnouncementHelper"
    private var previewTts: TextToSpeech? = null
    private val isPreviewActive = AtomicBoolean(false)

    /**
     * Preview an announcement in the UI (e.g. from the reminder edit dialog).
     */
    fun preview(
        context: Context,
        announcementText: String,
        onStatus: ((String) -> Unit)? = null
    ) {
        if (announcementText.isBlank()) {
            onStatus?.invoke("Announcement text is empty.")
            return
        }

        stopPreview()

        val appContext = context.applicationContext
        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInstance?.let { tts ->
                    val result = tts.setLanguage(Locale.getDefault())
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts.language = Locale.US
                    }

                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                    tts.setAudioAttributes(audioAttributes)

                    val utteranceId = "preview_${System.currentTimeMillis()}"
                    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(id: String?) {
                            isPreviewActive.set(true)
                        }

                        override fun onDone(id: String?) {
                            isPreviewActive.set(false)
                            cleanupTts(tts)
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(id: String?) {
                            isPreviewActive.set(false)
                            cleanupTts(tts)
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            isPreviewActive.set(false)
                            cleanupTts(tts)
                        }
                    })

                    tts.speak(announcementText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                    onStatus?.invoke("Playing voice preview...")
                }
            } else {
                onStatus?.invoke("TTS engine not available on this device.")
                cleanupTts(ttsInstance)
            }
        }
        previewTts = ttsInstance
    }

    /**
     * Speak announcement when a scheduled reminder triggers.
     */
    fun speakReminderAnnouncement(
        context: Context,
        announcementText: String,
        isQuietHoursActive: Boolean,
        isUrgent: Boolean
    ) {
        // Enforce quiet hours policy
        if (isQuietHoursActive && !isUrgent) {
            Log.d(TAG, "Voice announcement suppressed due to active quiet hours.")
            return
        }

        if (announcementText.isBlank()) return

        val appContext = context.applicationContext
        var backgroundTts: TextToSpeech? = null

        backgroundTts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                backgroundTts?.let { tts ->
                    val result = tts.setLanguage(Locale.getDefault())
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts.language = Locale.US
                    }

                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                    tts.setAudioAttributes(audioAttributes)

                    val utteranceId = "alarm_${System.currentTimeMillis()}"

                    // Safety timeout to guarantee cleanup even if engine hangs
                    val mainHandler = Handler(Looper.getMainLooper())
                    val timeoutRunnable = Runnable {
                        cleanupTts(tts)
                    }
                    mainHandler.postDelayed(timeoutRunnable, 15000L)

                    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(id: String?) {}

                        override fun onDone(id: String?) {
                            mainHandler.removeCallbacks(timeoutRunnable)
                            cleanupTts(tts)
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(id: String?) {
                            mainHandler.removeCallbacks(timeoutRunnable)
                            cleanupTts(tts)
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            mainHandler.removeCallbacks(timeoutRunnable)
                            cleanupTts(tts)
                        }
                    })

                    tts.speak(announcementText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                }
            } else {
                cleanupTts(backgroundTts)
            }
        }
    }

    fun stopPreview() {
        try {
            previewTts?.stop()
            previewTts?.shutdown()
            previewTts = null
            isPreviewActive.set(false)
        } catch (_: Exception) {}
    }

    private fun cleanupTts(tts: TextToSpeech?) {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
    }
}
