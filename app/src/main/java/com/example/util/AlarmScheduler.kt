package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.entity.ReminderEntity
import com.example.receiver.ReminderReceiver
import java.util.Calendar

object AlarmScheduler {

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true
        }
    }

    fun calculateNextTriggerMillis(timeMinutes: Int, repeatDays: String): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, timeMinutes / 60)
            set(Calendar.MINUTE, timeMinutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val isDaily = repeatDays.isBlank() || repeatDays.equals("DAILY", ignoreCase = true)
        if (isDaily) {
            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        }

        val daysMap = mapOf(
            Calendar.MONDAY to "MON",
            Calendar.TUESDAY to "TUE",
            Calendar.WEDNESDAY to "WED",
            Calendar.THURSDAY to "THU",
            Calendar.FRIDAY to "FRI",
            Calendar.SATURDAY to "SAT",
            Calendar.SUNDAY to "SUN"
        )
        val configuredDays = when {
            repeatDays.equals("WEEKDAYS", ignoreCase = true) -> listOf("MON", "TUE", "WED", "THU", "FRI")
            repeatDays.equals("WEEKENDS", ignoreCase = true) -> listOf("SAT", "SUN")
            else -> repeatDays.split(",").map { it.trim().uppercase() }
        }

        // Search next 7 days for the earliest active recurrence
        for (dayOffset in 0..7) {
            val candidate = (target.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            val dayAbbr = daysMap[candidate.get(Calendar.DAY_OF_WEEK)] ?: ""
            if (configuredDays.contains(dayAbbr)) {
                if (candidate.timeInMillis > now.timeInMillis) {
                    return candidate.timeInMillis
                }
            }
        }

        // Fallback to tomorrow if no candidate was matched
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }

    fun isWithinQuietHours(
        currentMinuteOfDay: Int,
        quietStartMinutes: Int,
        quietEndMinutes: Int
    ): Boolean {
        return if (quietStartMinutes <= quietEndMinutes) {
            currentMinuteOfDay in quietStartMinutes..quietEndMinutes
        } else {
            // Spanning midnight (e.g. 22:00 (1320) to 07:00 (420))
            currentMinuteOfDay >= quietStartMinutes || currentMinuteOfDay < quietEndMinutes
        }
    }

    fun scheduleAlarm(context: Context, reminder: ReminderEntity) {
        if (!reminder.isEnabled) {
            cancelAlarm(context, reminder.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerMillis = calculateNextTriggerMillis(reminder.timeMinutes, reminder.repeatDays)

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminder.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (canScheduleExactAlarms(context)) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            // Graceful fallback to inexact idle alarm if permission is missing
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun scheduleAllEnabledAlarms(context: Context, reminders: List<ReminderEntity>) {
        for (reminder in reminders) {
            if (reminder.isEnabled) {
                scheduleAlarm(context, reminder)
            }
        }
    }

    fun scheduleSnooze(context: Context, reminderId: Long, snoozeMinutes: Int = 10) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId.toInt() + 500000),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (canScheduleExactAlarms(context)) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }
}
