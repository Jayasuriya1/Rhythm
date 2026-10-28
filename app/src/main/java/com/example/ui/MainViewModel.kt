package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.ActiveTimerDraftEntity
import com.example.data.entity.HealthCheckinEntity
import com.example.data.entity.MedicationEntity
import com.example.data.entity.MedicationIntakeEntity
import com.example.data.entity.ReminderEntity
import com.example.data.entity.RoutineAnchorEntity
import com.example.data.entity.RoutineEntryEntity
import com.example.data.entity.SleepEntryEntity
import com.example.data.entity.StudyCategoryEntity
import com.example.data.entity.StudySessionEntity
import com.example.data.entity.StudyTaskEntity
import com.example.data.entity.WaterEntryEntity
import com.example.data.model.BreakPoint
import com.example.data.model.BreakScheduleMethod
import com.example.data.model.StudyProtocol
import com.example.data.model.TimerPhase
import com.example.data.repository.AppRepository
import com.example.util.AlarmScheduler
import com.example.util.DateTimeUtils
import com.example.util.NotificationHelper
import com.example.util.StudySoundHelper
import com.example.util.StudySoundType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppDestination(val label: String) {
    TODAY("Today"),
    ROUTINE("Routine"),
    HEALTH("Health"),
    STUDY("Study"),
    MEDITATION("Meditation"),
    BRAIN_DUMP("Brain Dump"),
    REMINDERS("Reminders"),
    ALARMS("Alarms"),
    INSIGHTS("Insights"),
    SETTINGS("Settings")
}

enum class TimerMode(val title: String, val defaultMinutes: Int) {
    POMODORO("Pomodoro (25m)", 25),
    DEEP_WORK("Deep Work (50m)", 50),
    CUSTOM("Custom Interval", 30),
    PROTOCOL("Protocol (with Breaks)", 50),
    STOPWATCH("Stopwatch Mode", 0)
}

data class TimerState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isCompleted: Boolean = false,
    val mode: TimerMode = TimerMode.POMODORO,
    val targetSeconds: Int = 25 * 60,
    val elapsedSeconds: Int = 0,
    val remainingSeconds: Int = 25 * 60,
    val startWallClockMillis: Long = 0L,
    val accumulatedElapsedSeconds: Int = 0,
    val sessionStartTimestamp: Long = 0L,
    val selectedTaskId: Long? = null,
    val selectedTaskTitle: String = "Focused Study",
    val selectedCategory: String = "General",

    // Protocol & Micro-Break State (Phase 1)
    val currentPhase: TimerPhase = TimerPhase.FOCUS,
    val currentPhaseElapsedSeconds: Int = 0,
    val currentPhaseTargetSeconds: Int = 25 * 60,
    val activeBreakPoint: BreakPoint? = null,
    val protocol: StudyProtocol = StudyProtocol(),
    val passedBreakPointIds: Set<String> = emptySet(),
    val activeBreakExtensionSeconds: Int = 0
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainViewModel(private val repository: AppRepository) : ViewModel() {

    private val _currentDestination = MutableStateFlow(AppDestination.TODAY)
    val currentDestination: StateFlow<AppDestination> = _currentDestination.asStateFlow()

    private val _selectedDate = MutableStateFlow(DateTimeUtils.todayDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Routine Data
    val routineAnchors: StateFlow<List<RoutineAnchorEntity>> = repository.routineAnchors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val routineEntriesForDate: StateFlow<List<RoutineEntryEntity>> = _selectedDate
        .flatMapLatest { date -> repository.getRoutineEntriesForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sleep Data
    val sleepForDate: StateFlow<SleepEntryEntity?> = _selectedDate
        .flatMapLatest { date -> repository.getSleepForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentSleepHistory: StateFlow<List<SleepEntryEntity>> = repository.getRecentSleepHistory(14)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Health Data
    val healthForDate: StateFlow<HealthCheckinEntity?> = _selectedDate
        .flatMapLatest { date -> repository.getHealthForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentHealthHistory: StateFlow<List<HealthCheckinEntity>> = repository.getRecentHealthHistory(14)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Water Data
    val waterEntriesForDate: StateFlow<List<WaterEntryEntity>> = _selectedDate
        .flatMapLatest { date -> repository.getWaterEntriesForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Medication Data
    // Medication Data
    val activeMedications: StateFlow<List<MedicationEntity>> = repository.activeMedications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMedications: StateFlow<List<MedicationEntity>> = repository.allMedications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedMedications: StateFlow<List<MedicationEntity>> = repository.archivedMedications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val distinctMedicationNames: StateFlow<List<String>> = repository.distinctMedicationNames
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val medicationIntakesForDate: StateFlow<List<MedicationIntakeEntity>> = _selectedDate
        .flatMapLatest { date -> repository.getMedicationIntakesForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentMedicationIntakes: StateFlow<List<MedicationIntakeEntity>> = repository.getRecentIntakeHistory(100)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Study Categories
    val allCategories: StateFlow<List<StudyCategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeCategories: StateFlow<List<StudyCategoryEntity>> = repository.activeCategories
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Study Tasks & Sessions
    val studyTasks: StateFlow<List<StudyTaskEntity>> = repository.allStudyTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyPriorityTask: StateFlow<StudyTaskEntity?> = repository.dailyPriorityTask
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentStudySessions: StateFlow<List<StudySessionEntity>> = repository.getRecentStudySessions(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Focus Timer State (Timestamp-anchored)
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()
    private var timerTickerJob: Job? = null

    // Brain Dump Notes
    val activeBrainDumpNotes: StateFlow<List<com.example.data.entity.BrainDumpNoteEntity>> = repository.activeBrainDumpNotes
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val archivedBrainDumpNotes: StateFlow<List<com.example.data.entity.BrainDumpNoteEntity>> = repository.archivedBrainDumpNotes
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _brainDumpSearchQuery = MutableStateFlow("")
    val brainDumpSearchQuery: StateFlow<String> = _brainDumpSearchQuery.asStateFlow()

    private val _brainDumpTagFilter = MutableStateFlow("All")
    val brainDumpTagFilter: StateFlow<String> = _brainDumpTagFilter.asStateFlow()

    // Reminders
    val allReminders: StateFlow<List<ReminderEntity>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val remindersMasterEnabled: StateFlow<Boolean> = repository.getSetting("reminders_master_enabled")
        .map { it?.value?.toBooleanStrictOrNull() ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val quietHoursEnabled: StateFlow<Boolean> = repository.getSetting("quiet_hours_enabled")
        .map { it?.value?.toBooleanStrictOrNull() ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val quietHoursStartMinutes: StateFlow<Int> = repository.getSetting("quiet_hours_start_minutes")
        .map { it?.value?.toIntOrNull() ?: 1320 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1320)

    val quietHoursEndMinutes: StateFlow<Int> = repository.getSetting("quiet_hours_end_minutes")
        .map { it?.value?.toIntOrNull() ?: 420 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 420)

    val quietHoursUrgentBypass: StateFlow<Boolean> = repository.getSetting("quiet_hours_urgent_bypass")
        .map { it?.value?.toBooleanStrictOrNull() ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Study Focus Timer Sounds (Independent settings)
    val studyStartSoundEnabled: StateFlow<Boolean> = repository.getSetting("study_start_sound_enabled")
        .map { it?.value?.toBooleanStrictOrNull() ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val studyBreakStartSoundEnabled: StateFlow<Boolean> = repository.getSetting("study_break_start_sound_enabled")
        .map { it?.value?.toBooleanStrictOrNull() ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val studyBreakEndSoundEnabled: StateFlow<Boolean> = repository.getSetting("study_break_end_sound_enabled")
        .map { it?.value?.toBooleanStrictOrNull() ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val studyEndSoundEnabled: StateFlow<Boolean> = repository.getSetting("study_end_sound_enabled")
        .map { it?.value?.toBooleanStrictOrNull() ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Keep screen awake during active timer
    val studyKeepScreenOnEnabled: StateFlow<Boolean> = repository.getSetting("study_keep_screen_on_enabled")
        .map { it?.value?.toBooleanStrictOrNull() ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Saved Protocol Configuration
    val studyProtocol: StateFlow<StudyProtocol> = repository.getSetting("saved_study_protocol")
        .map { setting -> StudyProtocol.fromJson(setting?.value) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, StudyProtocol())

    // App Theme Preference (OLED Pitch Black, Standard Dark, Light, Follow System)
    val appThemeMode: StateFlow<com.example.ui.theme.AppThemeMode> = repository.getSetting("app_theme_mode")
        .map { setting ->
            setting?.value?.let {
                try { com.example.ui.theme.AppThemeMode.valueOf(it) } catch (_: Exception) { null }
            } ?: com.example.ui.theme.AppThemeMode.SYSTEM
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.ui.theme.AppThemeMode.SYSTEM)

    fun setAppThemeMode(mode: com.example.ui.theme.AppThemeMode) {
        viewModelScope.launch {
            repository.saveSetting("app_theme_mode", mode.name)
        }
    }

    val keepScreenOnEnabled: StateFlow<Boolean> get() = studyKeepScreenOnEnabled
    val studySoundBreakStart: StateFlow<Boolean> get() = studyBreakStartSoundEnabled
    val breathingInhaleSeconds: StateFlow<Int> get() = meditationInhaleSeconds
    val breathingPauseSeconds: StateFlow<Int> get() = meditationHoldInhaleSeconds
    val breathingExhaleSeconds: StateFlow<Int> get() = meditationExhaleSeconds

    fun setStudyKeepScreenOnEnabled(enabled: Boolean) = setStudyKeepScreenOn(enabled)
    fun setKeepScreenOnEnabled(enabled: Boolean) = setStudyKeepScreenOn(enabled)

    fun setStudySoundEnabled(type: String, enabled: Boolean) {
        when (type) {
            "start" -> setStudyStartSoundEnabled(enabled)
            "break_start" -> setStudyBreakStartSoundEnabled(enabled)
            "break_end" -> setStudyBreakEndSoundEnabled(enabled)
            "end" -> setStudyEndSoundEnabled(enabled)
        }
    }

    // Decoupled Sound Events
    private val _studySoundEvents = MutableSharedFlow<StudySoundType>(extraBufferCapacity = 4)
    val studySoundEvents: SharedFlow<StudySoundType> = _studySoundEvents.asSharedFlow()

    // Meditation Data
    val allMeditationSessions: StateFlow<List<com.example.data.entity.MeditationSessionEntity>> = repository.allMeditationSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentMeditationSessions: StateFlow<List<com.example.data.entity.MeditationSessionEntity>> = repository.recentMeditationSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Meditation Breathing Settings (Persisted)
    val meditationAudioMode: StateFlow<com.example.util.MeditationAudioMode> = repository.getSetting("meditation_audio_mode")
        .map { setting ->
            setting?.value?.let { value ->
                try { com.example.util.MeditationAudioMode.valueOf(value) } catch (_: Exception) { null }
            } ?: com.example.util.MeditationAudioMode.TTS
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.util.MeditationAudioMode.TTS)

    val meditationInhaleSeconds: StateFlow<Int> = repository.getSetting("meditation_inhale_sec")
        .map { it?.value?.toIntOrNull() ?: 4 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4)

    val meditationHoldInhaleSeconds: StateFlow<Int> = repository.getSetting("meditation_hold_inhale_sec")
        .map { it?.value?.toIntOrNull() ?: 4 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4)

    val meditationExhaleSeconds: StateFlow<Int> = repository.getSetting("meditation_exhale_sec")
        .map { it?.value?.toIntOrNull() ?: 4 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4)

    val meditationHoldExhaleSeconds: StateFlow<Int> = repository.getSetting("meditation_hold_exhale_sec")
        .map { it?.value?.toIntOrNull() ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Dedicated Custom Alarms
    val allAlarms: StateFlow<List<com.example.data.entity.CustomAlarmEntity>> = repository.allAlarms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Message Toast / Feedback
    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage: StateFlow<String?> = _userFeedbackMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultAnchors()
            repository.ensureDefaultCategories()
            repository.ensureDefaultReminders()
            repository.ensureDefaultAlarms()
            repository.cleanupDuplicateReminders()
            restoreTimerStateFromDraft()
            refreshDatabaseIntegrityStats()
        }
    }

    suspend fun restoreTimerStateFromDraft() {
        val draft = repository.getTimerDraftSync() ?: return
        val mode = try {
            TimerMode.valueOf(draft.mode)
        } catch (_: Exception) {
            TimerMode.POMODORO
        }

        val phase = try {
            TimerPhase.valueOf(draft.currentPhase)
        } catch (_: Exception) {
            TimerPhase.FOCUS
        }

        val protocol = StudyProtocol.fromJson(draft.protocolJson)
        val passedIds = if (draft.passedBreakPointIds.isNotBlank()) {
            draft.passedBreakPointIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        } else emptySet()

        val activeBreakPoint = protocol.resolvedBreakPoints().firstOrNull { it.id in passedIds && (phase == TimerPhase.SHORT_BREAK || phase == TimerPhase.LONG_BREAK) }

        val now = System.currentTimeMillis()
        if (draft.isRunning && draft.startWallClockMillis > 0L) {
            val deltaSeconds = ((now - draft.startWallClockMillis) / 1000).toInt()

            if (phase == TimerPhase.FOCUS) {
                val totalElapsed = draft.accumulatedElapsedSeconds + maxOf(0, deltaSeconds)

                if (mode != TimerMode.STOPWATCH && totalElapsed >= draft.targetSeconds) {
                    _timerState.value = TimerState(
                        isRunning = false,
                        isPaused = false,
                        isCompleted = true,
                        mode = mode,
                        targetSeconds = draft.targetSeconds,
                        elapsedSeconds = draft.targetSeconds,
                        remainingSeconds = 0,
                        accumulatedElapsedSeconds = draft.targetSeconds,
                        sessionStartTimestamp = draft.startWallClockMillis,
                        selectedTaskId = draft.taskId,
                        selectedTaskTitle = draft.taskTitle,
                        selectedCategory = draft.category,
                        currentPhase = TimerPhase.COMPLETED,
                        currentPhaseElapsedSeconds = draft.targetSeconds,
                        currentPhaseTargetSeconds = draft.targetSeconds,
                        protocol = protocol,
                        passedBreakPointIds = passedIds
                    )
                } else {
                    _timerState.value = TimerState(
                        isRunning = true,
                        isPaused = false,
                        isCompleted = false,
                        mode = mode,
                        targetSeconds = draft.targetSeconds,
                        elapsedSeconds = totalElapsed,
                        remainingSeconds = if (mode == TimerMode.STOPWATCH) 0 else maxOf(0, draft.targetSeconds - totalElapsed),
                        startWallClockMillis = draft.startWallClockMillis,
                        accumulatedElapsedSeconds = draft.accumulatedElapsedSeconds,
                        sessionStartTimestamp = draft.startWallClockMillis,
                        selectedTaskId = draft.taskId,
                        selectedTaskTitle = draft.taskTitle,
                        selectedCategory = draft.category,
                        currentPhase = TimerPhase.FOCUS,
                        currentPhaseElapsedSeconds = totalElapsed,
                        currentPhaseTargetSeconds = draft.targetSeconds,
                        protocol = protocol,
                        passedBreakPointIds = passedIds
                    )
                    startTickerLoop()
                }
            } else {
                // Was in a break or cooldown
                val totalBreakElapsed = draft.phaseElapsedSeconds + maxOf(0, deltaSeconds)
                val targetBreak = draft.phaseTargetSeconds + draft.activeBreakExtensionSeconds

                if (totalBreakElapsed >= targetBreak) {
                    // Break completed while app was backgrounded
                    _timerState.value = TimerState(
                        isRunning = true,
                        isPaused = false,
                        isCompleted = false,
                        mode = mode,
                        targetSeconds = draft.targetSeconds,
                        elapsedSeconds = draft.accumulatedElapsedSeconds,
                        remainingSeconds = maxOf(0, draft.targetSeconds - draft.accumulatedElapsedSeconds),
                        startWallClockMillis = now,
                        accumulatedElapsedSeconds = draft.accumulatedElapsedSeconds,
                        sessionStartTimestamp = draft.startWallClockMillis,
                        selectedTaskId = draft.taskId,
                        selectedTaskTitle = draft.taskTitle,
                        selectedCategory = draft.category,
                        currentPhase = TimerPhase.FOCUS,
                        currentPhaseElapsedSeconds = draft.accumulatedElapsedSeconds,
                        currentPhaseTargetSeconds = draft.targetSeconds,
                        activeBreakPoint = null,
                        protocol = protocol,
                        passedBreakPointIds = passedIds,
                        activeBreakExtensionSeconds = 0
                    )
                    startTickerLoop()
                } else {
                    _timerState.value = TimerState(
                        isRunning = true,
                        isPaused = false,
                        isCompleted = false,
                        mode = mode,
                        targetSeconds = draft.targetSeconds,
                        elapsedSeconds = draft.accumulatedElapsedSeconds,
                        remainingSeconds = maxOf(0, draft.targetSeconds - draft.accumulatedElapsedSeconds),
                        startWallClockMillis = draft.startWallClockMillis,
                        accumulatedElapsedSeconds = draft.accumulatedElapsedSeconds,
                        sessionStartTimestamp = draft.startWallClockMillis,
                        selectedTaskId = draft.taskId,
                        selectedTaskTitle = draft.taskTitle,
                        selectedCategory = draft.category,
                        currentPhase = phase,
                        currentPhaseElapsedSeconds = totalBreakElapsed,
                        currentPhaseTargetSeconds = draft.phaseTargetSeconds,
                        activeBreakPoint = activeBreakPoint,
                        protocol = protocol,
                        passedBreakPointIds = passedIds,
                        activeBreakExtensionSeconds = draft.activeBreakExtensionSeconds
                    )
                    startTickerLoop()
                }
            }
        } else if (draft.isPaused) {
            _timerState.value = TimerState(
                isRunning = false,
                isPaused = true,
                isCompleted = false,
                mode = mode,
                targetSeconds = draft.targetSeconds,
                elapsedSeconds = draft.accumulatedElapsedSeconds,
                remainingSeconds = if (mode == TimerMode.STOPWATCH) 0 else maxOf(0, draft.targetSeconds - draft.accumulatedElapsedSeconds),
                startWallClockMillis = 0L,
                accumulatedElapsedSeconds = draft.accumulatedElapsedSeconds,
                sessionStartTimestamp = draft.startWallClockMillis,
                selectedTaskId = draft.taskId,
                selectedTaskTitle = draft.taskTitle,
                selectedCategory = draft.category,
                currentPhase = phase,
                currentPhaseElapsedSeconds = draft.phaseElapsedSeconds,
                currentPhaseTargetSeconds = draft.phaseTargetSeconds,
                activeBreakPoint = activeBreakPoint,
                protocol = protocol,
                passedBreakPointIds = passedIds,
                activeBreakExtensionSeconds = draft.activeBreakExtensionSeconds
            )
        }
    }

    fun navigateTo(destination: AppDestination) {
        _currentDestination.value = destination
    }

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    fun selectToday() {
        _selectedDate.value = DateTimeUtils.todayDateString()
    }

    fun clearFeedback() {
        _userFeedbackMessage.value = null
    }

    // --- Routine Actions ---
    fun markRoutineDoneNow(anchor: RoutineAnchorEntity) {
        val nowMinutes = DateTimeUtils.currentMinutesOfDay()
        viewModelScope.launch {
            repository.saveRoutineEntry(
                RoutineEntryEntity(
                    date = _selectedDate.value,
                    anchorId = anchor.id,
                    targetTimeMinutes = anchor.defaultTargetMinutes,
                    actualTimeMinutes = nowMinutes,
                    status = "COMPLETED"
                )
            )
            _userFeedbackMessage.value = "${anchor.name} recorded at ${DateTimeUtils.formatMinutesToTime(nowMinutes)}"
        }
    }

    fun markRoutineCustomTime(anchor: RoutineAnchorEntity, actualMinutes: Int, notes: String?) {
        viewModelScope.launch {
            repository.saveRoutineEntry(
                RoutineEntryEntity(
                    date = _selectedDate.value,
                    anchorId = anchor.id,
                    targetTimeMinutes = anchor.defaultTargetMinutes,
                    actualTimeMinutes = actualMinutes,
                    status = "COMPLETED",
                    notes = notes
                )
            )
            _userFeedbackMessage.value = "${anchor.name} recorded"
        }
    }

    fun markRoutineSkipped(anchor: RoutineAnchorEntity, reason: String?) {
        viewModelScope.launch {
            repository.saveRoutineEntry(
                RoutineEntryEntity(
                    date = _selectedDate.value,
                    anchorId = anchor.id,
                    targetTimeMinutes = anchor.defaultTargetMinutes,
                    actualTimeMinutes = null,
                    status = "SKIPPED",
                    notes = reason
                )
            )
            _userFeedbackMessage.value = "${anchor.name} marked as skipped"
        }
    }

    // --- Sleep Actions ---
    fun saveSleepRecord(bedtimeMin: Int, wakeTimeMin: Int, quality: Int, notes: String?) {
        val duration = if (wakeTimeMin >= bedtimeMin) {
            wakeTimeMin - bedtimeMin
        } else {
            (1440 - bedtimeMin) + wakeTimeMin
        }

        viewModelScope.launch {
            repository.saveSleepEntry(
                SleepEntryEntity(
                    date = _selectedDate.value,
                    bedtimeMinutes = bedtimeMin,
                    wakeTimeMinutes = wakeTimeMin,
                    durationMinutes = duration,
                    qualityRating = quality,
                    notes = notes
                )
            )
            _userFeedbackMessage.value = "Sleep recorded: ${DateTimeUtils.formatDurationMinutes(duration)}"
        }
    }

    fun deleteSleep() {
        viewModelScope.launch {
            repository.deleteSleep(_selectedDate.value)
            _userFeedbackMessage.value = "Sleep entry cleared"
        }
    }

    // --- Health Actions ---
    fun saveHealthCheckin(
        mood: Int?,
        energy: Int?,
        stress: Int?,
        irritability: Int?,
        activation: Int?,
        notes: String?
    ) {
        viewModelScope.launch {
            repository.saveHealthCheckin(
                HealthCheckinEntity(
                    date = _selectedDate.value,
                    mood = mood,
                    energy = energy,
                    stress = stress,
                    irritability = irritability,
                    activationEnergy = activation,
                    symptomsNotes = notes
                )
            )
            _userFeedbackMessage.value = "Health check-in saved"
        }
    }

    // --- Water Actions ---
    fun addWater(volumeMl: Int) {
        viewModelScope.launch {
            repository.addWater(_selectedDate.value, volumeMl)
            _userFeedbackMessage.value = "+${volumeMl}ml water logged"
        }
    }

    fun removeWaterEntry(id: Long) {
        viewModelScope.launch {
            repository.removeWaterEntry(id)
        }
    }

    // --- Medication Actions ---
    fun saveMedicationSchedule(
        id: Long = 0,
        name: String,
        dosage: String,
        unit: String,
        instructions: String,
        scheduledTimes: List<Int>,
        dosageForm: String? = "Tablet",
        repeatDays: String = "DAILY",
        startDate: String = DateTimeUtils.todayDateString(),
        endDate: String? = null,
        reminder: Boolean = false,
        notes: String? = null
    ) {
        val primaryTime = scheduledTimes.firstOrNull() ?: 480
        val timesJson = scheduledTimes.sorted().joinToString(",")

        val entity = MedicationEntity(
            id = id,
            name = name.trim(),
            dosage = dosage.trim(),
            unit = unit.trim(),
            instructions = instructions.trim(),
            scheduledTimeMinutes = primaryTime,
            reminderEnabled = reminder,
            isActive = true,
            notes = notes?.trim()?.ifBlank { null },
            dosageForm = dosageForm,
            scheduledTimesJson = timesJson,
            repeatDays = repeatDays,
            startDate = startDate.ifBlank { DateTimeUtils.todayDateString() },
            endDate = endDate?.ifBlank { null },
            isArchived = false
        )

        viewModelScope.launch {
            if (id == 0L) {
                repository.saveMedication(entity)
                _userFeedbackMessage.value = "Medication schedule created for ${entity.name}"
            } else {
                repository.updateMedication(entity)
                _userFeedbackMessage.value = "Updated schedule for ${entity.name}"
            }
        }
    }

    fun addMedication(
        name: String,
        dosage: String,
        unit: String,
        instructions: String,
        scheduledMinutes: Int,
        reminder: Boolean,
        notes: String?
    ) {
        saveMedicationSchedule(
            name = name,
            dosage = dosage,
            unit = unit,
            instructions = instructions,
            scheduledTimes = listOf(scheduledMinutes),
            reminder = reminder,
            notes = notes
        )
    }

    fun recordMedicationIntake(
        medication: MedicationEntity,
        scheduledTimeMinutes: Int = medication.scheduledTimeMinutes,
        status: String,
        notes: String? = null
    ) {
        viewModelScope.launch {
            val intake = MedicationIntakeEntity(
                date = _selectedDate.value,
                medicationId = medication.id,
                scheduledTimeMinutes = scheduledTimeMinutes,
                actualActionTime = System.currentTimeMillis(),
                status = status,
                notes = notes,
                medicationName = medication.name,
                dosageSnapshot = "${medication.dosage} ${medication.unit}",
                isCorrected = false,
                correctedAt = null
            )
            repository.recordMedicationIntake(intake)
            _userFeedbackMessage.value = "${medication.name} (${DateTimeUtils.formatMinutesToTime(scheduledTimeMinutes)}) marked as $status"
        }
    }

    fun correctMedicationIntake(
        intake: MedicationIntakeEntity,
        newStatus: String,
        newNotes: String? = null
    ) {
        viewModelScope.launch {
            val updated = intake.copy(
                status = newStatus,
                actualActionTime = System.currentTimeMillis(),
                notes = newNotes ?: intake.notes,
                isCorrected = true,
                correctedAt = System.currentTimeMillis()
            )
            repository.recordMedicationIntake(updated)
            _userFeedbackMessage.value = "Entry corrected to $newStatus"
        }
    }

    fun archiveMedication(id: Long, isArchived: Boolean) {
        viewModelScope.launch {
            repository.archiveMedication(id, isArchived)
            _userFeedbackMessage.value = if (isArchived) "Medication schedule archived (past history kept)" else "Medication restored"
        }
    }

    fun deleteMedication(id: Long) {
        viewModelScope.launch {
            repository.deleteMedication(id)
            _userFeedbackMessage.value = "Medication schedule deleted (past intakes preserved)"
        }
    }

    // --- Study Category Actions ---
    fun createStudyCategory(
        name: String,
        description: String?,
        colorHex: String,
        iconName: String
    ): Boolean {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            _userFeedbackMessage.value = "Category name cannot be empty"
            return false
        }

        val existing = allCategories.value.any { it.name.equals(trimmed, ignoreCase = true) }
        if (existing) {
            _userFeedbackMessage.value = "Category '$trimmed' already exists"
            return false
        }

        viewModelScope.launch {
            repository.createCategory(
                StudyCategoryEntity(
                    name = trimmed,
                    description = description?.trim(),
                    colorHex = colorHex,
                    iconName = iconName,
                    sortOrder = (allCategories.value.maxOfOrNull { it.sortOrder } ?: 0) + 1
                )
            )
            _userFeedbackMessage.value = "Category '$trimmed' created"
        }
        return true
    }

    suspend fun createStudyCategorySync(
        name: String,
        description: String?,
        colorHex: String,
        iconName: String
    ): Boolean {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return false
        val existing = repository.findCategoryByName(trimmed)
        if (existing != null) {
            _userFeedbackMessage.value = "Category '$trimmed' already exists"
            return false
        }
        repository.createCategory(
            StudyCategoryEntity(
                name = trimmed,
                description = description?.trim(),
                colorHex = colorHex,
                iconName = iconName,
                sortOrder = (allCategories.value.maxOfOrNull { it.sortOrder } ?: 0) + 1
            )
        )
        _userFeedbackMessage.value = "Category '$trimmed' created"
        return true
    }

    fun updateStudyCategory(category: StudyCategoryEntity): Boolean {
        val trimmed = category.name.trim()
        if (trimmed.isBlank()) {
            _userFeedbackMessage.value = "Category name cannot be empty"
            return false
        }

        val duplicate = allCategories.value.any {
            it.id != category.id && it.name.equals(trimmed, ignoreCase = true)
        }
        if (duplicate) {
            _userFeedbackMessage.value = "Category '$trimmed' already exists"
            return false
        }

        viewModelScope.launch {
            repository.updateCategory(category.copy(name = trimmed))
            _userFeedbackMessage.value = "Category updated"
        }
        return true
    }

    fun archiveStudyCategory(id: Long, isArchived: Boolean) {
        viewModelScope.launch {
            repository.archiveCategory(id, isArchived)
            _userFeedbackMessage.value = if (isArchived) "Category archived" else "Category restored"
        }
    }

    fun deleteStudyCategory(id: Long, categoryName: String, fallbackCategory: String = "General") {
        viewModelScope.launch {
            repository.deleteCategoryAndReassignTasks(id, categoryName, fallbackCategory)
            _userFeedbackMessage.value = "Category deleted. Tasks reassigned to '$fallbackCategory'"
        }
    }

    // --- Study Task Actions ---
    fun createStudyTask(
        title: String,
        category: String,
        estimatedMinutes: Int,
        isPriority: Boolean
    ) {
        viewModelScope.launch {
            val id = repository.saveStudyTask(
                StudyTaskEntity(
                    title = title.trim(),
                    category = category.trim().ifBlank { "General" },
                    estimatedMinutes = estimatedMinutes,
                    isDailyPriority = isPriority,
                    dateCreated = _selectedDate.value
                )
            )
            if (isPriority) {
                repository.setDailyPriority(id)
            }
            _userFeedbackMessage.value = "Study task added"
        }
    }

    fun editStudyTask(
        taskId: Long,
        newTitle: String,
        newCategory: String,
        newEstimatedMinutes: Int,
        isPriority: Boolean
    ) {
        viewModelScope.launch {
            val task = repository.getStudyTaskById(taskId) ?: return@launch
            repository.updateStudyTask(
                task.copy(
                    title = newTitle.trim(),
                    category = newCategory.trim().ifBlank { "General" },
                    estimatedMinutes = newEstimatedMinutes,
                    isDailyPriority = isPriority
                )
            )
            if (isPriority) {
                repository.setDailyPriority(taskId)
            }
            _userFeedbackMessage.value = "Task updated"
        }
    }

    fun updateTaskStatus(task: StudyTaskEntity, newStatus: String) {
        viewModelScope.launch {
            val completedDate = if (newStatus == "COMPLETED") _selectedDate.value else null
            repository.updateStudyTask(
                task.copy(
                    status = newStatus,
                    dateCompleted = completedDate
                )
            )
        }
    }

    fun toggleDailyPriority(task: StudyTaskEntity) {
        viewModelScope.launch {
            if (task.isDailyPriority) {
                repository.clearDailyPriority()
                _userFeedbackMessage.value = "Priority cleared"
            } else {
                repository.setDailyPriority(task.id)
                _userFeedbackMessage.value = "'${task.title}' set as Today's Priority"
            }
        }
    }

    fun setTaskSmallStep(task: StudyTaskEntity, step: String) {
        viewModelScope.launch {
            repository.updateStudyTask(task.copy(nextSmallStep = step))
            _userFeedbackMessage.value = "Next 5-min step saved"
        }
    }

    fun deleteStudyTask(id: Long) {
        viewModelScope.launch {
            repository.deleteStudyTask(id)
            _userFeedbackMessage.value = "Task deleted"
        }
    }

    // --- Study Focus Timer (Reliable Timestamp-anchored Engine) ---
    fun selectTaskForTimer(task: StudyTaskEntity?) {
        if (task != null) {
            _timerState.value = _timerState.value.copy(
                selectedTaskId = task.id,
                selectedTaskTitle = task.title,
                selectedCategory = task.category
            )
        } else {
            _timerState.value = _timerState.value.copy(
                selectedTaskId = null,
                selectedTaskTitle = "Focused Study",
                selectedCategory = "General"
            )
        }
        persistTimerDraft()
    }

    fun setTimerConfig(mode: TimerMode, customMinutes: Int = 25) {
        stopTimer()
        val totalSec = when (mode) {
            TimerMode.STOPWATCH -> 0
            TimerMode.PROTOCOL -> studyProtocol.value.totalFocusMinutes * 60
            else -> customMinutes * 60
        }
        _timerState.value = _timerState.value.copy(
            mode = mode,
            targetSeconds = totalSec,
            elapsedSeconds = 0,
            remainingSeconds = totalSec,
            accumulatedElapsedSeconds = 0,
            startWallClockMillis = 0L,
            isRunning = false,
            isPaused = false,
            isCompleted = false,
            currentPhase = TimerPhase.FOCUS,
            currentPhaseElapsedSeconds = 0,
            currentPhaseTargetSeconds = totalSec,
            activeBreakPoint = null,
            activeBreakExtensionSeconds = 0,
            passedBreakPointIds = emptySet(),
            protocol = studyProtocol.value
        )
        persistTimerDraft()
    }

    fun saveSetting(key: String, value: String) {
        viewModelScope.launch {
            repository.saveSetting(key, value)
        }
    }

    fun setStudyStartSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSetting("study_start_sound_enabled", enabled.toString())
        }
    }

    fun setStudyBreakStartSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSetting("study_break_start_sound_enabled", enabled.toString())
        }
    }

    fun setStudyBreakEndSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSetting("study_break_end_sound_enabled", enabled.toString())
        }
    }

    fun setStudyEndSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSetting("study_end_sound_enabled", enabled.toString())
        }
    }

    fun setStudyKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSetting("study_keep_screen_on_enabled", enabled.toString())
            _userFeedbackMessage.value = if (enabled) "Keep screen awake enabled during active timer" else "Keep screen awake disabled"
        }
    }

    fun saveStudyProtocol(protocol: StudyProtocol) {
        viewModelScope.launch {
            repository.saveSetting("saved_study_protocol", protocol.toJson())
            val current = _timerState.value
            _timerState.value = current.copy(protocol = protocol)
            if (current.mode == TimerMode.PROTOCOL && !current.isRunning && !current.isPaused) {
                setTimerConfig(TimerMode.PROTOCOL, protocol.totalFocusMinutes)
            }
            _userFeedbackMessage.value = "Study protocol saved (${protocol.totalFocusMinutes}m, ${protocol.resolvedBreakPoints().size} breaks)"
        }
    }

    fun previewStudySound(context: Context, type: StudySoundType) {
        StudySoundHelper.previewSound(context, type)
    }

    fun startTimer(context: Context? = null) {
        if (_timerState.value.isRunning) return

        val now = System.currentTimeMillis()
        val sessionStart = if (_timerState.value.sessionStartTimestamp == 0L) now else _timerState.value.sessionStartTimestamp

        _timerState.value = _timerState.value.copy(
            isRunning = true,
            isPaused = false,
            isCompleted = false,
            startWallClockMillis = now,
            sessionStartTimestamp = sessionStart
        )

        persistTimerDraft()
        startTickerLoop()

        if (studyStartSoundEnabled.value) {
            _studySoundEvents.tryEmit(StudySoundType.SESSION_START)
            if (context != null) {
                StudySoundHelper.playStartSound(context)
            }
        }
    }

    fun pauseTimer() {
        if (!_timerState.value.isRunning) return

        timerTickerJob?.cancel()
        val now = System.currentTimeMillis()
        val current = _timerState.value
        val deltaSeconds = if (current.startWallClockMillis > 0L) {
            ((now - current.startWallClockMillis) / 1000).toInt()
        } else 0

        if (current.currentPhase == TimerPhase.FOCUS) {
            val newAccumulated = current.accumulatedElapsedSeconds + maxOf(0, deltaSeconds)
            val remaining = if (current.mode == TimerMode.STOPWATCH) 0 else maxOf(0, current.targetSeconds - newAccumulated)

            _timerState.value = current.copy(
                isRunning = false,
                isPaused = true,
                startWallClockMillis = 0L,
                accumulatedElapsedSeconds = newAccumulated,
                elapsedSeconds = newAccumulated,
                remainingSeconds = remaining,
                currentPhaseElapsedSeconds = newAccumulated
            )
        } else {
            val newPhaseElapsed = current.currentPhaseElapsedSeconds + maxOf(0, deltaSeconds)
            _timerState.value = current.copy(
                isRunning = false,
                isPaused = true,
                startWallClockMillis = 0L,
                currentPhaseElapsedSeconds = newPhaseElapsed
            )
        }

        persistTimerDraft()
    }

    fun resumeTimer() {
        startTimer()
    }

    fun stopTimer() {
        timerTickerJob?.cancel()
        _timerState.value = _timerState.value.copy(
            isRunning = false,
            isPaused = false,
            isCompleted = false,
            elapsedSeconds = 0,
            remainingSeconds = _timerState.value.targetSeconds,
            accumulatedElapsedSeconds = 0,
            startWallClockMillis = 0L,
            sessionStartTimestamp = 0L,
            currentPhase = TimerPhase.FOCUS,
            currentPhaseElapsedSeconds = 0,
            activeBreakPoint = null,
            activeBreakExtensionSeconds = 0,
            passedBreakPointIds = emptySet()
        )
        viewModelScope.launch {
            repository.clearTimerDraft()
        }
    }

    fun skipBreak(context: Context? = null) {
        val current = _timerState.value
        if (current.currentPhase != TimerPhase.SHORT_BREAK && current.currentPhase != TimerPhase.LONG_BREAK && current.currentPhase != TimerPhase.COOLDOWN) return

        val now = System.currentTimeMillis()
        _timerState.value = current.copy(
            currentPhase = TimerPhase.FOCUS,
            currentPhaseElapsedSeconds = 0,
            activeBreakPoint = null,
            activeBreakExtensionSeconds = 0,
            startWallClockMillis = if (current.isRunning) now else 0L
        )
        persistTimerDraft()
        if (studyBreakEndSoundEnabled.value) {
            _studySoundEvents.tryEmit(StudySoundType.BREAK_END)
            if (context != null) {
                StudySoundHelper.playBreakEndSound(context)
            }
        }
        _userFeedbackMessage.value = "Skipped break. Resuming focus."
    }

    fun extendBreak(additionalMinutes: Int) {
        val current = _timerState.value
        val additionalSeconds = additionalMinutes * 60
        _timerState.value = current.copy(
            activeBreakExtensionSeconds = current.activeBreakExtensionSeconds + additionalSeconds
        )
        persistTimerDraft()
        _userFeedbackMessage.value = "Extended break by $additionalMinutes min"
    }

    private fun startTickerLoop() {
        timerTickerJob?.cancel()
        timerTickerJob = viewModelScope.launch {
            while (_timerState.value.isRunning) {
                delay(500)
                val current = _timerState.value
                if (!current.isRunning || current.startWallClockMillis == 0L) break

                val now = System.currentTimeMillis()
                val deltaSec = ((now - current.startWallClockMillis) / 1000).toInt()

                if (current.currentPhase == TimerPhase.FOCUS) {
                    val focusElapsed = current.accumulatedElapsedSeconds + maxOf(0, deltaSec)

                    // Check for break point triggers in PROTOCOL mode
                    if (current.mode == TimerMode.PROTOCOL) {
                        val breakPoints = current.protocol.resolvedBreakPoints()
                        val pendingBreak = breakPoints.firstOrNull { bp ->
                            focusElapsed >= bp.triggerElapsedMinute * 60 && !current.passedBreakPointIds.contains(bp.id)
                        }

                        if (pendingBreak != null) {
                            val newPassed = current.passedBreakPointIds + pendingBreak.id
                            val breakPhase = if (pendingBreak.isLongBreak) TimerPhase.LONG_BREAK else TimerPhase.SHORT_BREAK
                            val breakTargetSec = pendingBreak.breakDurationMinutes * 60

                            _timerState.value = current.copy(
                                accumulatedElapsedSeconds = focusElapsed,
                                elapsedSeconds = focusElapsed,
                                remainingSeconds = maxOf(0, current.targetSeconds - focusElapsed),
                                currentPhase = breakPhase,
                                currentPhaseElapsedSeconds = 0,
                                currentPhaseTargetSeconds = breakTargetSec,
                                activeBreakPoint = pendingBreak,
                                passedBreakPointIds = newPassed,
                                activeBreakExtensionSeconds = 0,
                                startWallClockMillis = now
                            )
                            persistTimerDraft()
                            if (studyBreakStartSoundEnabled.value) {
                                _studySoundEvents.tryEmit(StudySoundType.BREAK_START)
                            }
                            _userFeedbackMessage.value = "Time for a ${pendingBreak.label} (${pendingBreak.breakDurationMinutes}m). Step away and stretch!"
                            continue
                        }
                    }

                    if (current.mode != TimerMode.STOPWATCH && focusElapsed >= current.targetSeconds) {
                        if (current.mode == TimerMode.PROTOCOL && current.protocol.hasCooldownPeriod && current.currentPhase != TimerPhase.COOLDOWN) {
                            val cooldownSec = current.protocol.cooldownMinutes * 60
                            _timerState.value = current.copy(
                                accumulatedElapsedSeconds = current.targetSeconds,
                                elapsedSeconds = current.targetSeconds,
                                remainingSeconds = 0,
                                currentPhase = TimerPhase.COOLDOWN,
                                currentPhaseElapsedSeconds = 0,
                                currentPhaseTargetSeconds = cooldownSec,
                                startWallClockMillis = now
                            )
                            persistTimerDraft()
                            _userFeedbackMessage.value = "Focus completed! Entering ${current.protocol.cooldownMinutes}m session cooldown."
                            continue
                        } else {
                            _timerState.value = current.copy(
                                elapsedSeconds = current.targetSeconds,
                                remainingSeconds = 0,
                                accumulatedElapsedSeconds = current.targetSeconds,
                                isRunning = false,
                                isPaused = false,
                                isCompleted = true,
                                startWallClockMillis = 0L,
                                currentPhase = TimerPhase.COMPLETED
                            )
                            persistTimerDraft()
                            if (studyEndSoundEnabled.value) {
                                _studySoundEvents.tryEmit(StudySoundType.SESSION_END)
                            }
                            _userFeedbackMessage.value = "Focus session completed! Record your study log."
                            break
                        }
                    } else {
                        val remaining = if (current.mode == TimerMode.STOPWATCH) 0 else maxOf(0, current.targetSeconds - focusElapsed)
                        _timerState.value = current.copy(
                            elapsedSeconds = focusElapsed,
                            remainingSeconds = remaining,
                            currentPhaseElapsedSeconds = focusElapsed
                        )
                    }
                } else {
                    // In SHORT_BREAK, LONG_BREAK, or COOLDOWN
                    val breakElapsed = current.currentPhaseElapsedSeconds + maxOf(0, deltaSec)
                    val totalTarget = current.currentPhaseTargetSeconds + current.activeBreakExtensionSeconds

                    if (breakElapsed >= totalTarget) {
                        if (current.currentPhase == TimerPhase.COOLDOWN) {
                            _timerState.value = current.copy(
                                isRunning = false,
                                isPaused = false,
                                isCompleted = true,
                                startWallClockMillis = 0L,
                                currentPhase = TimerPhase.COMPLETED
                            )
                            persistTimerDraft()
                            if (studyEndSoundEnabled.value) {
                                _studySoundEvents.tryEmit(StudySoundType.SESSION_END)
                            }
                            _userFeedbackMessage.value = "Cooldown completed! Save your study session."
                            break
                        } else {
                            _timerState.value = current.copy(
                                currentPhase = TimerPhase.FOCUS,
                                currentPhaseElapsedSeconds = 0,
                                currentPhaseTargetSeconds = current.targetSeconds,
                                activeBreakPoint = null,
                                activeBreakExtensionSeconds = 0,
                                startWallClockMillis = now
                            )
                            persistTimerDraft()
                            if (studyBreakEndSoundEnabled.value) {
                                _studySoundEvents.tryEmit(StudySoundType.BREAK_END)
                            }
                            _userFeedbackMessage.value = "Break finished! Back to focus."
                        }
                    } else {
                        _timerState.value = current.copy(
                            currentPhaseElapsedSeconds = breakElapsed
                        )
                    }
                }
            }
        }
    }

    private fun persistTimerDraft() {
        viewModelScope.launch {
            val state = _timerState.value
            repository.saveTimerDraft(
                ActiveTimerDraftEntity(
                    id = 1,
                    taskId = state.selectedTaskId,
                    taskTitle = state.selectedTaskTitle,
                    category = state.selectedCategory,
                    mode = state.mode.name,
                    targetSeconds = state.targetSeconds,
                    startWallClockMillis = state.startWallClockMillis,
                    accumulatedElapsedSeconds = state.accumulatedElapsedSeconds,
                    isRunning = state.isRunning,
                    isPaused = state.isPaused,
                    isCompleted = state.isCompleted,
                    lastUpdatedMillis = System.currentTimeMillis(),
                    currentPhase = state.currentPhase.name,
                    phaseElapsedSeconds = state.currentPhaseElapsedSeconds,
                    phaseTargetSeconds = state.currentPhaseTargetSeconds,
                    protocolJson = state.protocol.toJson(),
                    passedBreakPointIds = state.passedBreakPointIds.joinToString(","),
                    activeBreakExtensionSeconds = state.activeBreakExtensionSeconds
                )
            )
        }
    }

    fun saveCompletedSession(notes: String?, status: String = "COMPLETED") {
        val state = _timerState.value
        val durationMins = maxOf(1, state.elapsedSeconds / 60)
        val now = System.currentTimeMillis()
        val startTimestamp = if (state.sessionStartTimestamp > 0L) state.sessionStartTimestamp else (now - state.elapsedSeconds * 1000L)

        viewModelScope.launch {
            repository.recordStudySession(
                StudySessionEntity(
                    taskId = state.selectedTaskId,
                    taskTitle = state.selectedTaskTitle,
                    category = state.selectedCategory,
                    durationMinutes = durationMins,
                    sessionType = state.mode.name,
                    date = _selectedDate.value,
                    timestamp = now,
                    startTimeMillis = startTimestamp,
                    endTimeMillis = now,
                    completionStatus = status,
                    learnedNotes = notes
                )
            )
            stopTimer()
            _userFeedbackMessage.value = "Saved $durationMins min study session ($status)"
        }
    }

    // --- Brain Dump Quick Capture Actions ---
    fun setBrainDumpSearchQuery(query: String) {
        _brainDumpSearchQuery.value = query
    }

    fun setBrainDumpTagFilter(tag: String) {
        _brainDumpTagFilter.value = tag
    }

    fun saveBrainDumpNote(
        id: Long? = null,
        title: String?,
        content: String,
        tag: String = "General",
        isPinned: Boolean = false
    ) {
        val trimmedContent = content.trim()
        if (trimmedContent.isBlank()) {
            _userFeedbackMessage.value = "Note content cannot be empty"
            return
        }

        viewModelScope.launch {
            if (id != null && id > 0L) {
                val existing = repository.getBrainDumpNoteById(id)
                if (existing != null) {
                    repository.updateBrainDumpNote(
                        existing.copy(
                            title = title?.trim()?.ifBlank { null },
                            content = trimmedContent,
                            tag = tag.trim().ifBlank { "General" },
                            isPinned = isPinned,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    _userFeedbackMessage.value = "Thought updated"
                    return@launch
                }
            }

            repository.saveBrainDumpNote(
                com.example.data.entity.BrainDumpNoteEntity(
                    title = title?.trim()?.ifBlank { null },
                    content = trimmedContent,
                    tag = tag.trim().ifBlank { "General" },
                    isPinned = isPinned,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            _userFeedbackMessage.value = "Thought captured"
        }
    }

    suspend fun saveBrainDumpNoteSync(
        title: String?,
        content: String,
        tag: String = "General",
        isPinned: Boolean = false
    ): Long {
        return repository.saveBrainDumpNote(
            com.example.data.entity.BrainDumpNoteEntity(
                title = title?.trim()?.ifBlank { null },
                content = content.trim(),
                tag = tag.trim().ifBlank { "General" },
                isPinned = isPinned,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    fun deleteBrainDumpNote(id: Long) {
        viewModelScope.launch {
            repository.deleteBrainDumpNote(id)
            _userFeedbackMessage.value = "Thought deleted"
        }
    }

    fun togglePinBrainDumpNote(note: com.example.data.entity.BrainDumpNoteEntity) {
        viewModelScope.launch {
            val newPinned = !note.isPinned
            repository.setBrainDumpNotePinned(note.id, newPinned)
            _userFeedbackMessage.value = if (newPinned) "Thought pinned to top" else "Thought unpinned"
        }
    }

    fun toggleArchiveBrainDumpNote(note: com.example.data.entity.BrainDumpNoteEntity) {
        viewModelScope.launch {
            val newArchived = !note.isArchived
            repository.setBrainDumpNoteArchived(note.id, newArchived)
            _userFeedbackMessage.value = if (newArchived) "Thought archived" else "Thought unarchived"
        }
    }

    fun convertBrainDumpNoteToStudyTask(
        note: com.example.data.entity.BrainDumpNoteEntity,
        category: String = "General",
        estimatedMinutes: Int = 25,
        isPriority: Boolean = false
    ) {
        viewModelScope.launch {
            convertBrainDumpNoteToStudyTaskSync(note, category, estimatedMinutes, isPriority)
        }
    }

    suspend fun convertBrainDumpNoteToStudyTaskSync(
        note: com.example.data.entity.BrainDumpNoteEntity,
        category: String = "General",
        estimatedMinutes: Int = 25,
        isPriority: Boolean = false
    ): Long {
        val taskId = repository.convertNoteToStudyTask(
            note = note,
            category = category,
            estimatedMinutes = estimatedMinutes
        )
        if (isPriority) {
            repository.setDailyPriority(taskId)
        }
        _userFeedbackMessage.value = "Converted to study task in '$category'"
        return taskId
    }

    // --- Reminder Center Operations ---
    fun saveReminder(
        reminder: ReminderEntity,
        context: Context? = null
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
        context: Context? = null
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
        context: Context? = null
    ) {
        viewModelScope.launch {
            repository.deleteReminder(reminder.id)
            if (context != null) {
                AlarmScheduler.cancelAlarm(context, reminder.id)
            }
            _userFeedbackMessage.value = "Reminder '${reminder.title}' deleted"
        }
    }

    // --- Dedicated Custom Alarm Operations ---
    fun saveCustomAlarm(
        context: Context,
        alarm: com.example.data.entity.CustomAlarmEntity
    ) {
        viewModelScope.launch {
            val id = repository.saveAlarm(alarm)
            val savedAlarm = if (alarm.id == 0L) alarm.copy(id = id) else alarm
            if (savedAlarm.isEnabled) {
                com.example.util.CustomAlarmScheduler.scheduleAlarm(context, savedAlarm)
            } else {
                com.example.util.CustomAlarmScheduler.cancelAlarm(context, savedAlarm.id)
            }
            _userFeedbackMessage.value = "Alarm '${alarm.title}' saved (${alarm.formattedTime()})"
        }
    }

    fun toggleAlarm(
        context: Context,
        alarm: com.example.data.entity.CustomAlarmEntity,
        isEnabled: Boolean
    ) {
        viewModelScope.launch {
            repository.setAlarmEnabled(alarm.id, isEnabled)
            if (isEnabled) {
                com.example.util.CustomAlarmScheduler.scheduleAlarm(context, alarm.copy(isEnabled = true))
            } else {
                com.example.util.CustomAlarmScheduler.cancelAlarm(context, alarm.id)
            }
            _userFeedbackMessage.value = if (isEnabled) "Alarm enabled" else "Alarm disabled"
        }
    }

    fun deleteCustomAlarm(
        context: Context,
        alarm: com.example.data.entity.CustomAlarmEntity
    ) {
        viewModelScope.launch {
            repository.deleteAlarm(alarm)
            com.example.util.CustomAlarmScheduler.cancelAlarm(context, alarm.id)
            _userFeedbackMessage.value = "Alarm '${alarm.title}' deleted"
        }
    }

    fun testAlarmNow(
        context: Context,
        alarm: com.example.data.entity.CustomAlarmEntity
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

    fun setRemindersMasterEnabled(enabled: Boolean, context: Context? = null) {
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

    fun syncFromRoutines(context: Context? = null) {
        viewModelScope.launch {
            repository.syncRoutinesToReminders()
            if (context != null) {
                val active = repository.getEnabledRemindersSync()
                AlarmScheduler.scheduleAllEnabledAlarms(context, active)
            }
            _userFeedbackMessage.value = "Synced reminders from Social Rhythm anchors"
        }
    }

    fun syncFromMedications(context: Context? = null) {
        viewModelScope.launch {
            repository.syncMedicationsToReminders()
            if (context != null) {
                val active = repository.getEnabledRemindersSync()
                AlarmScheduler.scheduleAllEnabledAlarms(context, active)
            }
            _userFeedbackMessage.value = "Synced reminders from Medication schedules"
        }
    }

    fun testSendNotification(context: Context) {
        NotificationHelper.showTestNotification(context)
        _userFeedbackMessage.value = "Test notification sent"
    }

    // --- Settings, Backup & Database Integrity ---
    private val _databaseIntegrityStats = MutableStateFlow<com.example.util.DatabaseIntegrityStats?>(null)
    val databaseIntegrityStats: StateFlow<com.example.util.DatabaseIntegrityStats?> = _databaseIntegrityStats.asStateFlow()

    fun refreshDatabaseIntegrityStats() {
        viewModelScope.launch {
            _databaseIntegrityStats.value = repository.getDatabaseIntegrityStats()
        }
    }

    fun exportBackupJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportDataBackup()
            _userFeedbackMessage.value = "Backup generated successfully"
            onResult(json)
        }
    }

    fun restoreBackupJson(
        jsonString: String,
        context: Context? = null,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.restoreDataBackup(jsonString)
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

    fun cleanDuplicateAlarms(context: Context? = null) {
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
                com.example.data.entity.MeditationSessionEntity(
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

    fun setMeditationAudioMode(mode: com.example.util.MeditationAudioMode) {
        viewModelScope.launch {
            repository.saveSetting("meditation_audio_mode", mode.name)
        }
    }

    fun setMeditationAudioMode(modeName: String) {
        val mode = try { com.example.util.MeditationAudioMode.valueOf(modeName) } catch (_: Exception) { com.example.util.MeditationAudioMode.TTS }
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

    fun clearAllUserData() {
        viewModelScope.launch {
            stopTimer()
            repository.clearAllData()
            refreshDatabaseIntegrityStats()
            _userFeedbackMessage.value = "All data cleared and reinitialized"
        }
    }
}
