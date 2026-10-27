package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.ui.screens.alarm.ActiveAlarmActivity
import com.example.util.CustomAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CustomAlarmPlaybackService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var crescendoJob: Job? = null

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var currentAlarmId: Long = 0L
    private var currentAlarmTitle: String = "Alarm"
    private var isBackupAlarm: Boolean = false

    companion object {
        private const val TAG = "CustomAlarmPlayback"
        const val CHANNEL_ID = "custom_alarm_active_channel"
        const val NOTIFICATION_ID = 99991

        const val ACTION_START = "com.example.action.START_CUSTOM_ALARM"
        const val ACTION_STOP = "com.example.action.STOP_CUSTOM_ALARM"

        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_AUDIO_URI = "extra_audio_uri"
        const val EXTRA_CRESCENDO_SECONDS = "extra_crescendo_seconds"
        const val EXTRA_IS_LOOPING = "extra_is_looping"
        const val EXTRA_VIBRATE = "extra_vibrate"
        const val EXTRA_IS_BACKUP = "extra_is_backup"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initVibrator()
        acquireWakeLock()
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                "rhythmfocus:active_alarm_wakelock"
            )?.apply {
                acquire(15 * 60 * 1000L) // 15 min safety timeout
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release wakeLock", e)
        }
    }

    private fun initVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START -> {
                currentAlarmId = intent.getLongExtra(EXTRA_ALARM_ID, 0L)
                currentAlarmTitle = intent.getStringExtra(EXTRA_TITLE) ?: "Alarm"
                val audioUriStr = intent.getStringExtra(EXTRA_AUDIO_URI)
                val crescendoSeconds = intent.getIntExtra(EXTRA_CRESCENDO_SECONDS, 60)
                val isLooping = intent.getBooleanExtra(EXTRA_IS_LOOPING, true)
                val vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, true)
                isBackupAlarm = intent.getBooleanExtra(EXTRA_IS_BACKUP, false)

                startForegroundServiceNotification()
                startPlayback(audioUriStr, crescendoSeconds, isLooping, isBackupAlarm)

                if (vibrate) {
                    startVibration(isBackupAlarm)
                }
            }
            ACTION_STOP -> {
                val stoppedId = intent.getLongExtra(EXTRA_ALARM_ID, currentAlarmId)
                CustomAlarmScheduler.cancelChainedBackupAlarm(applicationContext, stoppedId)
                stopAlarm()
            }
        }

        return START_NOT_STICKY
    }

    private fun startForegroundServiceNotification() {
        // Full screen intent to launch ActiveAlarmActivity over the lock screen
        val fullScreenIntent = Intent(this, ActiveAlarmActivity::class.java).apply {
            putExtra(ActiveAlarmActivity.EXTRA_ALARM_ID, currentAlarmId)
            putExtra(ActiveAlarmActivity.EXTRA_IS_BACKUP, isBackupAlarm)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID + 1,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val titleText = if (isBackupAlarm) "🚨 BACKUP ALARM ACTIVE!" else "⏰ $currentAlarmTitle"
        val contentText = if (isBackupAlarm) {
            "Backup alarm firing! Complete mission to silence."
        } else {
            "Alarm ringing. Tap or unlock to complete mission."
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_rhythm)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startPlayback(audioUriStr: String?, crescendoSeconds: Int, isLooping: Boolean, isBackup: Boolean) {
        requestAudioFocus()
        stopAudio()

        val parsedUri: Uri = try {
            if (!audioUriStr.isNullOrBlank()) {
                Uri.parse(audioUriStr)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
        } catch (_: Exception) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(applicationContext, parsedUri)
                this.isLooping = isLooping
                prepare()
            }
            mediaPlayer = player

            if (isBackup || crescendoSeconds <= 0) {
                // Immediate full volume for backup alarms or when crescendo is disabled
                player.setVolume(1.0f, 1.0f)
                player.start()
            } else {
                // Linear volume fade-in (crescendo) over crescendoSeconds window
                player.setVolume(0.01f, 0.01f)
                player.start()
                startCrescendo(player, crescendoSeconds)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing custom audio, falling back to system default alarm", e)
            playFallbackTone(isBackup)
        }
    }

    private fun startCrescendo(player: MediaPlayer, totalSeconds: Int) {
        crescendoJob?.cancel()
        crescendoJob = serviceScope.launch {
            val steps = maxOf(10, totalSeconds)
            val stepDelayMs = (totalSeconds * 1000L) / steps
            for (step in 1..steps) {
                if (!isActive) break
                val volume = (step.toFloat() / steps).coerceIn(0.01f, 1.0f)
                try {
                    player.setVolume(volume, volume)
                } catch (_: Exception) {
                    break
                }
                delay(stepDelayMs)
            }
            try {
                player.setVolume(1.0f, 1.0f)
            } catch (_: Exception) {}
        }
    }

    private fun playFallbackTone(isBackup: Boolean) {
        try {
            val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(applicationContext, fallbackUri)
                isLooping = true
                setVolume(1.0f, 1.0f)
                prepare()
                start()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Fatal: failed to play fallback ringtone", e)
        }
    }

    private fun startVibration(isBackup: Boolean) {
        try {
            val pattern = if (isBackup) {
                // Intense repeating bursts for backup alarm
                longArrayOf(0, 300, 100, 300, 100, 600, 200)
            } else {
                // Calmer, rhythmic pulsing
                longArrayOf(0, 500, 500, 500, 500, 1000, 500)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern, 0)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start vibration", e)
        }
    }

    private fun requestAudioFocus() {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAcceptsDelayedFocusGain(false)
                .build()
            audioFocusRequest = focusRequest
            audioManager?.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(
                null,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }
    }

    private fun stopAudio() {
        crescendoJob?.cancel()
        crescendoJob = null
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.reset()
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio player", e)
        } finally {
            mediaPlayer = null
        }
    }

    private fun stopAlarm() {
        stopAudio()
        try {
            vibrator?.cancel()
        } catch (_: Exception) {}

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        }

        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Active Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows active ringing alarm notifications and full-screen overlay"
                setSound(null, null) // Audio is handled independently by MediaPlayer
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudio()
        releaseWakeLock()
    }
}
