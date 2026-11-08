package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.database.AppDatabase
import com.example.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val incomingAction = intent.action ?: return
        if (incomingAction == Intent.ACTION_BOOT_COMPLETED ||
            incomingAction == Intent.ACTION_MY_PACKAGE_REPLACED ||
            incomingAction == Intent.ACTION_TIMEZONE_CHANGED ||
            incomingAction == Intent.ACTION_TIME_CHANGED
        ) {
            val pendingResult = try { goAsync() } catch (_: Exception) { null }
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    rescheduleAllAlarms(context, db)

                    // Restore floating Brain Dump bubble if opted in by user and permission granted
                    if (incomingAction == Intent.ACTION_BOOT_COMPLETED || incomingAction == Intent.ACTION_MY_PACKAGE_REPLACED) {
                        val overlayEnabledSetting = db.settingsDao().getSetting("floating_overlay_enabled").firstOrNull()
                        val overlayEnabled = overlayEnabledSetting?.value?.toBooleanStrictOrNull() ?: false
                        if (overlayEnabled && android.provider.Settings.canDrawOverlays(context)) {
                            val serviceIntent = Intent(context, com.example.service.FloatingBrainDumpService::class.java).apply {
                                setAction(com.example.service.FloatingBrainDumpService.ACTION_START)
                            }
                            androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
                        }
                    }
                } catch (_: Exception) {
                    // Safe handling
                } finally {
                    pendingResult?.finish()
                }
            }
        }
    }

    companion object {
        suspend fun rescheduleAllAlarms(context: Context, db: AppDatabase) {
            // Verify master switch is enabled
            val masterEnabledSetting = db.settingsDao().getSetting("reminders_master_enabled").firstOrNull()
            val masterEnabled = masterEnabledSetting?.value?.toBooleanStrictOrNull() ?: true

            if (masterEnabled) {
                val enabledReminders = db.reminderDao().getEnabledRemindersSync()
                AlarmScheduler.scheduleAllEnabledAlarms(context, enabledReminders)
            }

            // Restore all enabled dedicated custom alarms
            val enabledAlarms = db.customAlarmDao().getAllAlarmsSync().filter { it.isEnabled }
            for (alarm in enabledAlarms) {
                com.example.util.CustomAlarmScheduler.scheduleAlarm(context, alarm)
            }
        }
    }
}
