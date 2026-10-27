package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.MainActivity
import com.example.data.entity.CustomAlarmEntity
import com.example.receiver.CustomAlarmReceiver
import java.util.Calendar

object CustomAlarmScheduler {

    private const val TAG = "CustomAlarmScheduler"
    const val EXTRA_ALARM_ID = "extra_custom_alarm_id"
    const val EXTRA_ALARM_TITLE = "extra_custom_alarm_title"
    const val EXTRA_IS_BACKUP = "extra_is_backup_alarm"
    const val BACKUP_ALARM_OFFSET_MS = 5 * 60 * 1000L // 5 minutes exactly

    fun scheduleAlarm(context: Context, alarm: CustomAlarmEntity) {
        if (!alarm.isEnabled) {
            cancelAlarm(context, alarm.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Exact alarm permission not granted for alarm ${alarm.id}")
            }
        }

        val triggerMillis = calculateNextTriggerMillis(alarm.timeMinutes, alarm.repeatDays)

        val intent = Intent(context, CustomAlarmReceiver::class.java).apply {
            action = "com.example.action.CUSTOM_ALARM_TRIGGER"
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_ALARM_TITLE, alarm.title)
            putExtra(EXTRA_IS_BACKUP, false)
        }

        val requestCode = (100000 + alarm.id).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Show intent for setAlarmClock (opens main activity or active alarm)
        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            requestCode + 50000,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val clockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
            alarmManager.setAlarmClock(clockInfo, pendingIntent)
            Log.i(TAG, "Scheduled exact alarm ${alarm.id} for $triggerMillis (${alarm.formattedTime()})")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling exact alarm", e)
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } catch (e2: Exception) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        }
    }

    fun cancelAlarm(context: Context, alarmId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, CustomAlarmReceiver::class.java).apply {
            action = "com.example.action.CUSTOM_ALARM_TRIGGER"
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val requestCode = (100000 + alarmId).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.i(TAG, "Cancelled alarm $alarmId")
        }
        cancelChainedBackupAlarm(context, alarmId)
    }

    /**
     * Schedules an aggressive fallback backup alarm exactly 5 minutes after the primary alarm triggers.
     */
    fun scheduleChainedBackupAlarm(context: Context, primaryAlarmId: Long, primaryAlarmTitle: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val backupTriggerMillis = System.currentTimeMillis() + BACKUP_ALARM_OFFSET_MS

        val intent = Intent(context, CustomAlarmReceiver::class.java).apply {
            action = "com.example.action.CUSTOM_ALARM_TRIGGER"
            putExtra(EXTRA_ALARM_ID, primaryAlarmId)
            putExtra(EXTRA_ALARM_TITLE, primaryAlarmTitle)
            putExtra(EXTRA_IS_BACKUP, true)
        }

        val requestCode = (200000 + primaryAlarmId).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            requestCode + 50000,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val clockInfo = AlarmManager.AlarmClockInfo(backupTriggerMillis, showPendingIntent)
            alarmManager.setAlarmClock(clockInfo, pendingIntent)
            Log.i(TAG, "Scheduled chained backup alarm for primary alarm $primaryAlarmId at $backupTriggerMillis")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling chained backup alarm", e)
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, backupTriggerMillis, pendingIntent)
            } catch (_: Exception) {}
        }
    }

    fun cancelChainedBackupAlarm(context: Context, primaryAlarmId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, CustomAlarmReceiver::class.java).apply {
            action = "com.example.action.CUSTOM_ALARM_TRIGGER"
            putExtra(EXTRA_ALARM_ID, primaryAlarmId)
            putExtra(EXTRA_IS_BACKUP, true)
        }
        val requestCode = (200000 + primaryAlarmId).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.i(TAG, "Cancelled chained backup alarm for primary $primaryAlarmId")
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

        // If time already passed today, advance at least to tomorrow
        if (target.before(now) || target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Check weekday matching
        for (i in 0 until 7) {
            val dayOfWeek = target.get(Calendar.DAY_OF_WEEK)
            val dayAbbr = when (dayOfWeek) {
                Calendar.MONDAY -> "MON"
                Calendar.TUESDAY -> "TUE"
                Calendar.WEDNESDAY -> "WED"
                Calendar.THURSDAY -> "THU"
                Calendar.FRIDAY -> "FRI"
                Calendar.SATURDAY -> "SAT"
                Calendar.SUNDAY -> "SUN"
                else -> "MON"
            }

            if (matchesDay(repeatDays, dayAbbr)) {
                return target.timeInMillis
            }
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        return target.timeInMillis
    }

    private fun matchesDay(repeatDays: String, dayAbbr: String): Boolean {
        if (repeatDays.equals("DAILY", ignoreCase = true) || repeatDays.isBlank()) return true
        if (repeatDays.equals("WEEKDAYS", ignoreCase = true)) {
            return listOf("MON", "TUE", "WED", "THU", "FRI").contains(dayAbbr)
        }
        if (repeatDays.equals("WEEKENDS", ignoreCase = true)) {
            return listOf("SAT", "SUN").contains(dayAbbr)
        }
        val days = repeatDays.split(",").map { it.trim().uppercase() }
        return days.contains(dayAbbr)
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            return alarmManager?.canScheduleExactAlarms() ?: false
        }
        return true
    }
}
