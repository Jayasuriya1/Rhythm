package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.database.AppDatabase
import com.example.util.AlarmScheduler
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_REMINDER = "com.example.ACTION_TRIGGER_REMINDER"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val reminder = db.reminderDao().getReminderById(reminderId)

                if (reminder != null && reminder.isEnabled) {
                    // Check master switch
                    val masterEnabledSetting = db.settingsDao().getSetting("reminders_master_enabled").firstOrNull()
                    val masterEnabled = masterEnabledSetting?.value?.toBooleanStrictOrNull() ?: true

                    if (masterEnabled) {
                        // Check quiet hours
                        val quietHoursEnabled = db.settingsDao().getSetting("quiet_hours_enabled").firstOrNull()?.value?.toBooleanStrictOrNull() ?: true
                        val quietStartMinutes = db.settingsDao().getSetting("quiet_hours_start_minutes").firstOrNull()?.value?.toIntOrNull() ?: 1320 // 22:00
                        val quietEndMinutes = db.settingsDao().getSetting("quiet_hours_end_minutes").firstOrNull()?.value?.toIntOrNull() ?: 420 // 07:00

                        val now = Calendar.getInstance()
                        val currentMinuteOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                        val inQuietHours = quietHoursEnabled && AlarmScheduler.isWithinQuietHours(
                            currentMinuteOfDay,
                            quietStartMinutes,
                            quietEndMinutes
                        )

                        // If in quiet hours and not urgent, NotificationHelper will silence sound/vibrate
                        NotificationHelper.showReminderNotification(
                            context = context,
                            reminder = reminder,
                            isQuietHoursActive = inQuietHours
                        )

                        // Trigger audio playback service if custom audio or wake-up alarm is enabled
                        if ((!inQuietHours || reminder.isUrgent) && reminder.soundEnabled) {
                            if (reminder.audioPlaybackMode == "CUSTOM_SINGLE" ||
                                reminder.audioPlaybackMode == "CUSTOM_RANDOM" ||
                                reminder.category.equals("WAKE_UP", ignoreCase = true)
                            ) {
                                val serviceIntent = Intent(context, com.example.service.AlarmPlaybackService::class.java).apply {
                                    action = com.example.service.AlarmPlaybackService.ACTION_PLAY_ALARM
                                    putExtra(com.example.service.AlarmPlaybackService.EXTRA_REMINDER_ID, reminder.id)
                                }
                                androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
                            }
                        }

                        // Voice announcement if enabled (disabled by default, respects quiet hours)
                        if (reminder.voiceAnnouncementEnabled) {
                            val announcement = reminder.effectiveVoiceAnnouncement()
                            com.example.util.VoiceAnnouncementHelper.speakReminderAnnouncement(
                                context = context,
                                announcementText = announcement,
                                isQuietHoursActive = inQuietHours,
                                isUrgent = reminder.isUrgent
                            )
                        }
                    }

                    // Reschedule for next recurrence
                    AlarmScheduler.scheduleAlarm(context, reminder)
                }
            } catch (_: Exception) {
                // Log and gracefully finish
            } finally {
                pendingResult.finish()
            }
        }
    }
}
