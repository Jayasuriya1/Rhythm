package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.database.AppDatabase
import com.example.service.CustomAlarmPlaybackService
import com.example.ui.screens.alarm.ActiveAlarmActivity
import com.example.util.CustomAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CustomAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CustomAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(CustomAlarmScheduler.EXTRA_ALARM_ID, 0L)
        val alarmTitle = intent.getStringExtra(CustomAlarmScheduler.EXTRA_ALARM_TITLE) ?: "Alarm"
        val isBackup = intent.getBooleanExtra(CustomAlarmScheduler.EXTRA_IS_BACKUP, false)

        Log.i(TAG, "Custom alarm received: id=$alarmId, title=$alarmTitle, isBackup=$isBackup")

        // 1. WakeLock & Screen Activation: Physically light up the screen immediately
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val screenWakeLock = powerManager?.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                "rhythmfocus:alarm_receiver_wakelock"
            )
            screenWakeLock?.acquire(3 * 60 * 1000L) // 3 minute screen activation
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring wake lock in CustomAlarmReceiver", e)
        }

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val alarm = if (alarmId > 0) db.customAlarmDao().getAlarmByIdSync(alarmId) else null

                val title = alarm?.title ?: alarmTitle
                val audioUri = alarm?.customAudioUri
                val crescendoSeconds = if (isBackup) 0 else (alarm?.crescendoDurationSeconds ?: 60)
                val isLooping = alarm?.isLooping ?: true
                val vibrate = alarm?.vibrateEnabled ?: true

                // 2. Start Foreground Audio Playback Service
                val serviceIntent = Intent(context, CustomAlarmPlaybackService::class.java).apply {
                    action = CustomAlarmPlaybackService.ACTION_START
                    putExtra(CustomAlarmPlaybackService.EXTRA_ALARM_ID, alarmId)
                    putExtra(CustomAlarmPlaybackService.EXTRA_TITLE, title)
                    putExtra(CustomAlarmPlaybackService.EXTRA_AUDIO_URI, audioUri)
                    putExtra(CustomAlarmPlaybackService.EXTRA_CRESCENDO_SECONDS, crescendoSeconds)
                    putExtra(CustomAlarmPlaybackService.EXTRA_IS_LOOPING, isLooping)
                    putExtra(CustomAlarmPlaybackService.EXTRA_VIBRATE, vibrate)
                    putExtra(CustomAlarmPlaybackService.EXTRA_IS_BACKUP, isBackup)
                }
                ContextCompat.startForegroundService(context, serviceIntent)

                // 3. Launch Full-Screen Lockscreen Overlay Activity
                val activityIntent = Intent(context, ActiveAlarmActivity::class.java).apply {
                    putExtra(ActiveAlarmActivity.EXTRA_ALARM_ID, alarmId)
                    putExtra(ActiveAlarmActivity.EXTRA_IS_BACKUP, isBackup)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                context.startActivity(activityIntent)

                // 4. Chained Backup Alarm: If this is the primary alarm and chained backup is enabled, schedule fallback in 5m
                if (!isBackup && (alarm?.chainedBackupAlarmEnabled != false)) {
                    CustomAlarmScheduler.scheduleChainedBackupAlarm(context, alarmId, title)
                }

                // 5. Reschedule recurring alarm for next occurrence if enabled
                if (alarm != null && alarm.isEnabled && !isBackup) {
                    CustomAlarmScheduler.scheduleAlarm(context, alarm)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in CustomAlarmReceiver handling", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
