package com.example.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.MeditationSessionEntity
import com.example.data.repository.AppRepository
import com.example.sync.CloudSyncManager
import com.example.ui.theme.AppThemeMode
import com.example.util.AlarmScheduler
import com.example.util.DatabaseIntegrityStats
import com.example.util.MeditationAudioMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: AppRepository,
    private val context: Context? = null
) : ViewModel() {

    private val appContext: Context? get() = (context ?: com.example.data.database.AppDatabase.appContext)?.applicationContext

    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage: StateFlow<String?> = _userFeedbackMessage.asStateFlow()

    private val _databaseIntegrityStats = MutableStateFlow<DatabaseIntegrityStats?>(null)
    val databaseIntegrityStats: StateFlow<DatabaseIntegrityStats?> = _databaseIntegrityStats.asStateFlow()

    val cloudSyncManager: CloudSyncManager? by lazy {
        appContext?.let { CloudSyncManager(it, repository) }
    }

    val appThemeMode: StateFlow<AppThemeMode> = repository.getSetting("app_theme_mode")
        .map { setting ->
            setting?.value?.let {
                try { AppThemeMode.valueOf(it) } catch (_: Exception) { null }
            } ?: AppThemeMode.OLED_DARK
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppThemeMode.OLED_DARK)

    fun clearFeedbackMessage() {
        _userFeedbackMessage.value = null
    }

    fun setAppThemeMode(mode: AppThemeMode): kotlinx.coroutines.Job = viewModelScope.launch {
        repository.saveSetting("app_theme_mode", mode.name)
    }

    fun refreshDatabaseIntegrityStats() {
        viewModelScope.launch {
            _databaseIntegrityStats.value = repository.getDatabaseIntegrityStats()
        }
    }

    fun exportBackupJson(passphrase: String? = null, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportDataBackup(passphrase)
            _userFeedbackMessage.value = if (passphrase.isNullOrBlank()) "Backup generated successfully" else "Encrypted backup generated successfully"
            onResult(json)
        }
    }

    fun restoreBackupJson(
        jsonString: String,
        context: Context? = appContext,
        remapAutoIds: Boolean = false,
        passphrase: String? = null,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.restoreDataBackup(jsonString, remapAutoIds, passphrase)
            if (result.isSuccess) {
                val summary = result.getOrThrow()
                refreshDatabaseIntegrityStats()
                if (context != null) {
                    val activeReminders = repository.getEnabledRemindersSync()
                    AlarmScheduler.scheduleAllEnabledAlarms(context, activeReminders)
                }
                _userFeedbackMessage.value = "Data restored successfully (${summary.routineEntriesCount} routines, ${summary.medicationsCount} meds)"
                onComplete(true, "Restored successfully: ${summary.routineEntriesCount} routines, ${summary.sleepCount} sleep records, ${summary.medicationsCount} meds, ${summary.studySessionsCount} study sessions.")
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to restore backup"
                _userFeedbackMessage.value = "Error: $errorMsg"
                onComplete(false, errorMsg)
            }
        }
    }

    fun isBackupEncrypted(jsonString: String): Boolean {
        return repository.isBackupEncrypted(jsonString)
    }

    fun validateBackupJson(jsonString: String, passphrase: String? = null): Result<com.example.util.BackupSummary> {
        return repository.validateBackup(jsonString, passphrase)
    }

    fun cleanDuplicateAlarms(context: Context? = appContext) {
        viewModelScope.launch {
            val deleted = repository.cleanupDuplicateReminders(context)
            refreshDatabaseIntegrityStats()
            _userFeedbackMessage.value = if (deleted > 0) {
                "Cleaned up $deleted duplicate reminder alarms"
            } else {
                "Reminders are verified: no duplicate alarms found"
            }
        }
    }

    fun saveMeditationSession(
        techniqueId: String,
        title: String,
        durationMinutes: Int,
        reflectionNotes: String? = null
    ) {
        viewModelScope.launch {
            repository.saveMeditationSession(
                MeditationSessionEntity(
                    techniqueId = techniqueId,
                    title = title,
                    durationMinutes = durationMinutes,
                    completedAt = System.currentTimeMillis(),
                    reflectionNotes = reflectionNotes?.trim()?.ifBlank { null }
                )
            )
            refreshDatabaseIntegrityStats()
            _userFeedbackMessage.value = "Mindfulness session completed ($durationMinutes min)"
        }
    }

    fun setMeditationAudioMode(mode: MeditationAudioMode) {
        viewModelScope.launch {
            repository.saveSetting("meditation_audio_mode", mode.name)
        }
    }

    fun setMeditationAudioMode(modeName: String) {
        val mode = try { MeditationAudioMode.valueOf(modeName) } catch (_: Exception) { MeditationAudioMode.TTS }
        setMeditationAudioMode(mode)
    }

    fun setMeditationBreathingPacing(inhale: Int, holdInhale: Int, exhale: Int, holdExhale: Int) {
        viewModelScope.launch {
            repository.saveSetting("meditation_inhale_sec", inhale.toString())
            repository.saveSetting("meditation_hold_inhale_sec", holdInhale.toString())
            repository.saveSetting("meditation_exhale_sec", exhale.toString())
            repository.saveSetting("meditation_hold_exhale_sec", holdExhale.toString())
        }
    }

    fun setBreathingDurations(inhale: Int, pause: Int, exhale: Int) {
        setMeditationBreathingPacing(inhale, pause, exhale, pause)
    }
}

