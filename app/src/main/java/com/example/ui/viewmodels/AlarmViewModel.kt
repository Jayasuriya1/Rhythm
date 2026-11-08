package com.example.ui.viewmodels

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.CustomAlarmEntity
import com.example.data.entity.RegisteredCodeEntity
import com.example.data.entity.ReminderEntity
import com.example.data.repository.AppRepository
import com.example.util.AlarmScheduler
import com.example.util.CustomAlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmViewModel(
    private val repository: AppRepository,
    private val context: Context? = null
) : ViewModel() {

    private val appContext: Context? get() = (context ?: com.example.data.database.AppDatabase.appContext)?.applicationContext

    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage: StateFlow<String?> = _userFeedbackMessage.asStateFlow()

    val allReminders: StateFlow<List<ReminderEntity>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<ReminderEntity>> get() = allReminders

    val customAlarms: StateFlow<List<CustomAlarmEntity>> = repository.allAlarms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val registeredCodes: StateFlow<List<RegisteredCodeEntity>> = repository.allRegisteredCodes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearFeedbackMessage() {
        _userFeedbackMessage.value = null
    }

    // --- Reminders ---
    fun saveReminder(
        reminder: ReminderEntity,
        context: Context? = appContext
    ) {
        viewModelScope.launch {
            val id = repository.saveReminder(reminder)
            val updatedReminder = if (reminder.id == 0L) reminder.copy(id = id) else reminder
            if (context != null && updatedReminder.isEnabled) {
                AlarmScheduler.scheduleAlarm(context, updatedReminder)
            } else if (context != null && !updatedReminder.isEnabled) {
                AlarmScheduler.cancelAlarm(context, updatedReminder.id)
            }
            _userFeedbackMessage.value = "Reminder '${reminder.title}' saved"
        }
    }

    fun toggleReminderEnabled(
        reminder: ReminderEntity,
        isEnabled: Boolean,
        context: Context? = appContext
    ) {
        viewModelScope.launch {
            repository.setReminderEnabled(reminder.id, isEnabled)
            if (context != null) {
                if (isEnabled) {
                    AlarmScheduler.scheduleAlarm(context, reminder.copy(isEnabled = true))
                } else {
                    AlarmScheduler.cancelAlarm(context, reminder.id)
                }
            }
            _userFeedbackMessage.value = if (isEnabled) "Reminder enabled" else "Reminder disabled"
        }
    }

    fun deleteReminder(
        reminder: ReminderEntity,
        context: Context? = appContext
    ) {
        viewModelScope.launch {
            repository.deleteReminder(reminder.id)
            if (context != null) {
                AlarmScheduler.cancelAlarm(context, reminder.id)
            }
            _userFeedbackMessage.value = "Reminder '${reminder.title}' deleted"
        }
    }

    fun setRemindersMasterEnabled(enabled: Boolean, context: Context? = appContext) {
        viewModelScope.launch {
            repository.saveSetting("reminders_master_enabled", enabled.toString())
            if (context != null) {
                if (enabled) {
                    val activeReminders = repository.getEnabledRemindersSync()
                    AlarmScheduler.scheduleAllEnabledAlarms(context, activeReminders)
                } else {
                    val allRemindersList = repository.getEnabledRemindersSync()
                    for (rem in allRemindersList) {
                        AlarmScheduler.cancelAlarm(context, rem.id)
                    }
                }
            }
            _userFeedbackMessage.value = if (enabled) "Reminders activated" else "All reminders paused"
        }
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSetting("quiet_hours_enabled", enabled.toString())
            _userFeedbackMessage.value = if (enabled) "Quiet hours active" else "Quiet hours turned off"
        }
    }

    fun setQuietHoursTime(startMinutes: Int, endMinutes: Int) {
        viewModelScope.launch {
            repository.saveSetting("quiet_hours_start_minutes", startMinutes.toString())
            repository.saveSetting("quiet_hours_end_minutes", endMinutes.toString())
            _userFeedbackMessage.value = "Quiet hours schedule updated"
        }
    }

    fun setQuietHoursUrgentBypass(bypass: Boolean) {
        viewModelScope.launch {
            repository.saveSetting("quiet_hours_urgent_bypass", bypass.toString())
        }
    }

    fun syncFromRoutines(context: Context? = appContext) {
        viewModelScope.launch {
            repository.syncRoutinesToReminders()
            if (context != null) {
                val active = repository.getEnabledRemindersSync()
                AlarmScheduler.scheduleAllEnabledAlarms(context, active)
            }
            _userFeedbackMessage.value = "Synced reminders from Social Rhythm anchors"
        }
    }

    fun syncFromMedications(context: Context? = appContext) {
        viewModelScope.launch {
            repository.syncMedicationsToReminders()
            if (context != null) {
                val active = repository.getEnabledRemindersSync()
                AlarmScheduler.scheduleAllEnabledAlarms(context, active)
            }
            _userFeedbackMessage.value = "Synced reminders from medication schedule"
        }
    }

    // --- Dedicated Custom Alarms ---
    fun saveCustomAlarm(
        context: Context,
        alarm: CustomAlarmEntity
    ) {
        viewModelScope.launch {
            val id = repository.saveAlarm(alarm)
            val savedAlarm = if (alarm.id == 0L) alarm.copy(id = id) else alarm
            if (savedAlarm.isEnabled) {
                CustomAlarmScheduler.scheduleAlarm(context, savedAlarm)
            } else {
                CustomAlarmScheduler.cancelAlarm(context, savedAlarm.id)
            }
            _userFeedbackMessage.value = "Alarm '${alarm.title}' saved (${alarm.formattedTime()})"
        }
    }

    fun toggleAlarm(
        context: Context,
        alarm: CustomAlarmEntity,
        isEnabled: Boolean
    ) {
        viewModelScope.launch {
            repository.setAlarmEnabled(alarm.id, isEnabled)
            if (isEnabled) {
                CustomAlarmScheduler.scheduleAlarm(context, alarm.copy(isEnabled = true))
            } else {
                CustomAlarmScheduler.cancelAlarm(context, alarm.id)
            }
            _userFeedbackMessage.value = if (isEnabled) "Alarm enabled" else "Alarm disabled"
        }
    }

    fun deleteCustomAlarm(
        context: Context,
        alarm: CustomAlarmEntity
    ) {
        viewModelScope.launch {
            repository.deleteAlarm(alarm)
            CustomAlarmScheduler.cancelAlarm(context, alarm.id)
            _userFeedbackMessage.value = "Alarm '${alarm.title}' deleted"
        }
    }

    fun testAlarmNow(
        context: Context,
        alarm: CustomAlarmEntity
    ) {
        val serviceIntent = Intent(context, com.example.service.CustomAlarmPlaybackService::class.java).apply {
            action = com.example.service.CustomAlarmPlaybackService.ACTION_START
            putExtra(com.example.service.CustomAlarmPlaybackService.EXTRA_ALARM_ID, alarm.id)
            putExtra(com.example.service.CustomAlarmPlaybackService.EXTRA_TITLE, alarm.title)
            putExtra(com.example.service.CustomAlarmPlaybackService.EXTRA_AUDIO_URI, alarm.customAudioUri)
            putExtra(com.example.service.CustomAlarmPlaybackService.EXTRA_CRESCENDO_SECONDS, alarm.crescendoDurationSeconds)
            putExtra(com.example.service.CustomAlarmPlaybackService.EXTRA_IS_LOOPING, alarm.isLooping)
            putExtra(com.example.service.CustomAlarmPlaybackService.EXTRA_VIBRATE, alarm.vibrateEnabled)
            putExtra(com.example.service.CustomAlarmPlaybackService.EXTRA_IS_BACKUP, false)
        }
        androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)

        val activityIntent = Intent(context, com.example.ui.screens.alarm.ActiveAlarmActivity::class.java).apply {
            putExtra(com.example.ui.screens.alarm.ActiveAlarmActivity.EXTRA_ALARM_ID, alarm.id)
            putExtra(com.example.ui.screens.alarm.ActiveAlarmActivity.EXTRA_IS_BACKUP, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(activityIntent)
    }

    fun testAlarmAfterDelay(
        context: Context,
        alarm: CustomAlarmEntity,
        delaySeconds: Int = 5
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
        val triggerMillis = System.currentTimeMillis() + (delaySeconds * 1000L)
        val intent = Intent(context, com.example.receiver.CustomAlarmReceiver::class.java).apply {
            action = "com.example.action.CUSTOM_ALARM_TRIGGER"
            putExtra(CustomAlarmScheduler.EXTRA_ALARM_ID, alarm.id)
            putExtra(CustomAlarmScheduler.EXTRA_ALARM_TITLE, alarm.title)
            putExtra(CustomAlarmScheduler.EXTRA_IS_BACKUP, false)
        }
        val requestCode = 999999
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val showIntent = Intent(context, com.example.MainActivity::class.java)
        val showPendingIntent = android.app.PendingIntent.getActivity(
            context,
            requestCode + 1,
            showIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        try {
            val clockInfo = android.app.AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
            alarmManager.setAlarmClock(clockInfo, pendingIntent)
            _userFeedbackMessage.value = "Real alarm scheduled in $delaySeconds sec! Lock screen or minimize to verify."
        } catch (e: Exception) {
            try {
                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                _userFeedbackMessage.value = "Real alarm scheduled in $delaySeconds sec! Lock screen or minimize to verify."
            } catch (e2: Exception) {
                _userFeedbackMessage.value = "Could not schedule exact alarm: ${e2.message}"
            }
        }
    }

    // --- Registered Codes ---
    fun saveRegisteredCode(
        code: RegisteredCodeEntity,
        onSaved: ((Long) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val id = repository.saveRegisteredCode(code)
            _userFeedbackMessage.value = "Saved code '${code.label}'"
            onSaved?.invoke(id)
        }
    }

    fun deleteRegisteredCode(code: RegisteredCodeEntity) {
        viewModelScope.launch {
            repository.deleteRegisteredCode(code.id)
            _userFeedbackMessage.value = "Deleted code '${code.label}'"
        }
    }

    suspend fun checkCodeDuplicate(codeValue: String): RegisteredCodeEntity? {
        return repository.getRegisteredCodeByValue(codeValue.trim())
    }

    fun recordCodeScanned(id: Long) {
        viewModelScope.launch {
            repository.updateRegisteredCodeScanned(id)
        }
    }
}

