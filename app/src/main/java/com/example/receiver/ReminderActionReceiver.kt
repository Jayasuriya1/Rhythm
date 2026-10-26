package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.database.AppDatabase
import com.example.data.entity.MedicationIntakeEntity
import com.example.data.entity.WaterEntryEntity
import com.example.util.DateTimeUtils
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TAKE_MEDICATION = "com.example.ACTION_TAKE_MEDICATION"
        const val ACTION_LOG_WATER = "com.example.ACTION_LOG_WATER"
        const val ACTION_STOP_ALARM = "com.example.ACTION_STOP_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.example.ACTION_SNOOZE_ALARM"

        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_REFERENCE_ID = "extra_reference_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_VOLUME_ML = "extra_volume_ml"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (notificationId != -1) {
            NotificationHelper.cancelNotification(context, notificationId)
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val today = DateTimeUtils.todayDateString()

                when (intent.action) {
                    ACTION_TAKE_MEDICATION -> {
                        val refId = intent.getStringExtra(EXTRA_REFERENCE_ID) ?: ""
                        // format: "med:<id>_<timeIndex>"
                        val medId = if (refId.startsWith("med:")) {
                            refId.removePrefix("med:").split("_").firstOrNull()?.toLongOrNull()
                        } else null

                        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
                        val reminder = if (reminderId != -1L) db.reminderDao().getReminderById(reminderId) else null

                        if (medId != null) {
                            val med = db.medicationDao().getAllMedications().firstOrNull()?.find { it.id == medId }
                            val scheduledTime = reminder?.timeMinutes ?: (med?.scheduledTimeMinutes ?: 480)
                            val intake = MedicationIntakeEntity(
                                date = today,
                                medicationId = medId,
                                scheduledTimeMinutes = scheduledTime,
                                actualActionTime = System.currentTimeMillis(),
                                status = "TAKEN",
                                notes = "Logged via reminder notification",
                                medicationName = med?.name ?: (reminder?.title ?: "Medication"),
                                dosageSnapshot = if (med != null) "${med.dosage} ${med.unit}".trim() else ""
                            )
                            db.medicationDao().insertOrUpdateIntake(intake)
                        } else if (reminder != null) {
                            // General or ad-hoc medication reminder
                            val intake = MedicationIntakeEntity(
                                date = today,
                                medicationId = 0L,
                                scheduledTimeMinutes = reminder.timeMinutes,
                                actualActionTime = System.currentTimeMillis(),
                                status = "TAKEN",
                                notes = "Logged via reminder notification",
                                medicationName = reminder.title,
                                dosageSnapshot = ""
                            )
                            db.medicationDao().insertOrUpdateIntake(intake)
                        }
                    }

                    ACTION_LOG_WATER -> {
                        val volumeMl = intent.getIntExtra(EXTRA_VOLUME_ML, 250)
                        val entry = WaterEntryEntity(
                            date = today,
                            volumeMl = volumeMl,
                            timestamp = System.currentTimeMillis()
                        )
                        db.waterDao().insertWaterEntry(entry)
                    }

                    ACTION_STOP_ALARM -> {
                        val serviceIntent = Intent(context, com.example.service.AlarmPlaybackService::class.java).apply {
                            action = com.example.service.AlarmPlaybackService.ACTION_STOP_ALARM
                        }
                        context.startService(serviceIntent)
                    }

                    ACTION_SNOOZE_ALARM -> {
                        val remId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
                        val serviceIntent = Intent(context, com.example.service.AlarmPlaybackService::class.java).apply {
                            action = com.example.service.AlarmPlaybackService.ACTION_SNOOZE_ALARM
                            putExtra(com.example.service.AlarmPlaybackService.EXTRA_REMINDER_ID, remId)
                        }
                        context.startService(serviceIntent)
                    }
                }
            } catch (_: Exception) {
                // Catch any exception safely
            } finally {
                pendingResult.finish()
            }
        }
    }
}
