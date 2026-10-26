package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.database.AppDatabase
import com.example.data.entity.ReminderEntity
import com.example.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

class AlarmPlaybackService : Service() {

    companion object {
        const val ACTION_PLAY_ALARM = "com.example.service.ACTION_PLAY_ALARM"
        const val ACTION_STOP_ALARM = "com.example.service.ACTION_STOP_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.example.service.ACTION_SNOOZE_ALARM"

        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        private const val CHANNEL_ID = "channel_alarm_playback"
        private const val NOTIFICATION_ID = 8888
    }

    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var currentReminderId: Long = -1L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_ALARM -> {
                stopAlarmPlayback()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SNOOZE_ALARM -> {
                val remId = intent.getLongExtra(EXTRA_REMINDER_ID, currentReminderId)
                if (remId != -1L) {
                    AlarmScheduler.scheduleSnooze(applicationContext, remId, 10)
                }
                stopAlarmPlayback()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PLAY_ALARM -> {
                val remId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
                if (remId != -1L) {
                    currentReminderId = remId
                    startAlarm(remId)
                } else {
                    stopSelf()
                }
            }
            else -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startAlarm(reminderId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val reminder = db.reminderDao().getReminderById(reminderId)
            if (reminder == null) {
                stopSelf()
                return@launch
            }

            // Post foreground notification immediately
            val notification = buildForegroundNotification(reminder)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            // Request Audio Focus
            requestAudioFocus()

            // Determine audio source
            setupAndPlayAudio(reminder, db)
        }
    }

    private fun setupAndPlayAudio(reminder: ReminderEntity, db: AppDatabase) {
        releasePlayer()
        var played = false

        when (reminder.audioPlaybackMode) {
            "CUSTOM_SINGLE" -> {
                if (!reminder.customAudioUri.isNullOrBlank()) {
                    played = tryPlayUri(Uri.parse(reminder.customAudioUri))
                }
            }
            "CUSTOM_RANDOM" -> {
                val collection = reminder.getAudioCollection()
                if (collection.isNotEmpty()) {
                    val available = if (collection.size > 1 && reminder.lastPlayedAudioUri != null) {
                        collection.filter { it.first != reminder.lastPlayedAudioUri }
                    } else collection

                    val chosen = (if (available.isNotEmpty()) available else collection).random(Random.Default)
                    played = tryPlayUri(Uri.parse(chosen.first))
                    if (played) {
                        CoroutineScope(Dispatchers.IO).launch {
                            db.reminderDao().updateReminder(reminder.copy(lastPlayedAudioUri = chosen.first))
                        }
                    }
                }
            }
        }

        // Fallback to default system alarm / notification ringtone if custom not played
        if (!played) {
            val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (defaultUri != null) {
                tryPlayUri(defaultUri)
            }
        }
    }

    private fun tryPlayUri(uri: Uri): Boolean {
        return try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(applicationContext, uri)
                isLooping = true
                prepare()
                start()
            }
            mediaPlayer = player
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun requestAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener { /* handle focus changes if needed */ }
                .build()
            focusRequest = req
            am.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { am.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(null)
        }
    }

    private fun stopAlarmPlayback() {
        releasePlayer()
        abandonAudioFocus()
    }

    private fun releasePlayer() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Audio Playback",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Foreground playback controls and actions for active alarms"
                setSound(null, null)
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(reminder: ReminderEntity): android.app.Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            reminder.id.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AlarmPlaybackService::class.java).apply {
            action = ACTION_STOP_ALARM
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            reminder.id.toInt() + 1000,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, AlarmPlaybackService::class.java).apply {
            action = ACTION_SNOOZE_ALARM
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        val snoozePendingIntent = PendingIntent.getService(
            this,
            reminder.id.toInt() + 2000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val audioInfo = when (reminder.audioPlaybackMode) {
            "CUSTOM_SINGLE" -> reminder.customAudioTitle ?: "Custom Audio"
            "CUSTOM_RANDOM" -> "Random Audio Track"
            else -> "Alarm Sound"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_rhythm)
            .setContentTitle("🔔 Alarm: ${reminder.title}")
            .setContentText("Playing $audioInfo • Scheduled for ${reminder.formattedTime()}")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_stat_rhythm, "Stop Alarm", stopPendingIntent)
            .addAction(R.drawable.ic_stat_rhythm, "Snooze (10m)", snoozePendingIntent)
            .build()
    }

    override fun onDestroy() {
        stopAlarmPlayback()
        super.onDestroy()
    }
}
