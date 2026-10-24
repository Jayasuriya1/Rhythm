package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.HealthCheckinEntity
import com.example.data.entity.MedicationEntity
import com.example.data.entity.MedicationIntakeEntity
import com.example.data.entity.RoutineAnchorEntity
import com.example.data.entity.RoutineEntryEntity
import com.example.data.entity.SleepEntryEntity
import com.example.data.entity.StudySessionEntity
import com.example.data.entity.StudyTaskEntity
import com.example.data.entity.WaterEntryEntity
import com.example.data.repository.AppRepository
import com.example.util.DateTimeUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppDestination(val label: String) {
    TODAY("Today"),
    ROUTINE("Routine"),
    HEALTH("Health"),
    STUDY("Study"),
    INSIGHTS("Insights"),
    SETTINGS("Settings")
}

enum class TimerMode(val title: String, val defaultMinutes: Int) {
    POMODORO("Pomodoro (25m)", 25),
    DEEP_WORK("Deep Work (50m)", 50),
    CUSTOM("Custom Interval", 30),
    STOPWATCH("Stopwatch Mode", 0)
}

data class TimerState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val mode: TimerMode = TimerMode.POMODORO,
    val targetSeconds: Int = 25 * 60,
    val elapsedSeconds: Int = 0,
    val selectedTaskId: Long? = null,
    val selectedTaskTitle: String = "Focused Study",
    val selectedCategory: String = "General"
)

data class TodayDashboardData(
    val greeting: String,
    val dateDisplay: String,
    val completedRoutinesCount: Int,
    val totalRoutinesCount: Int,
    val sleepSummary: SleepEntryEntity?,
    val healthCheckin: HealthCheckinEntity?,
    val waterTotalMl: Int,
    val waterTargetMl: Int,
    val upcomingMedication: Pair<MedicationEntity, MedicationIntakeEntity?>?,
    val dailyPriorityTask: StudyTaskEntity?
)

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
    val activeMedications: StateFlow<List<MedicationEntity>> = repository.activeMedications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMedications: StateFlow<List<MedicationEntity>> = repository.allMedications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val medicationIntakesForDate: StateFlow<List<MedicationIntakeEntity>> = _selectedDate
        .flatMapLatest { date -> repository.getMedicationIntakesForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Study Tasks & Sessions
    val studyTasks: StateFlow<List<StudyTaskEntity>> = repository.allStudyTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyPriorityTask: StateFlow<StudyTaskEntity?> = repository.dailyPriorityTask
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentStudySessions: StateFlow<List<StudySessionEntity>> = repository.getRecentStudySessions(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Focus Timer State
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()
    private var timerJob: Job? = null

    // Message Toast / Feedback
    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage: StateFlow<String?> = _userFeedbackMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultAnchors()
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

    fun clearRoutineEntry(id: Long) {
        viewModelScope.launch {
            repository.saveRoutineEntry(
                RoutineEntryEntity(
                    id = id,
                    date = _selectedDate.value,
                    anchorId = "",
                    targetTimeMinutes = 0,
                    actualTimeMinutes = null,
                    status = "UNRECORDED"
                )
            )
        }
    }

    // --- Sleep Actions ---
    fun saveSleepRecord(bedtimeMin: Int, wakeTimeMin: Int, quality: Int, notes: String?) {
        // Calculate duration accounting for crossing midnight
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
    fun addMedication(
        name: String,
        dosage: String,
        unit: String,
        instructions: String,
        scheduledMinutes: Int,
        reminder: Boolean,
        notes: String?
    ) {
        viewModelScope.launch {
            repository.saveMedication(
                MedicationEntity(
                    name = name,
                    dosage = dosage,
                    unit = unit,
                    instructions = instructions,
                    scheduledTimeMinutes = scheduledMinutes,
                    reminderEnabled = reminder,
                    notes = notes
                )
            )
            _userFeedbackMessage.value = "Medication added"
        }
    }

    fun recordMedicationIntake(medication: MedicationEntity, status: String, notes: String?) {
        viewModelScope.launch {
            repository.recordMedicationIntake(
                MedicationIntakeEntity(
                    date = _selectedDate.value,
                    medicationId = medication.id,
                    scheduledTimeMinutes = medication.scheduledTimeMinutes,
                    actualActionTime = System.currentTimeMillis(),
                    status = status,
                    notes = notes
                )
            )
            _userFeedbackMessage.value = "${medication.name} marked as $status"
        }
    }

    fun deleteMedication(id: Long) {
        viewModelScope.launch {
            repository.deleteMedication(id)
            _userFeedbackMessage.value = "Medication removed"
        }
    }

    // --- Study Actions ---
    fun createStudyTask(
        title: String,
        category: String,
        estimatedMinutes: Int,
        isPriority: Boolean
    ) {
        viewModelScope.launch {
            val id = repository.saveStudyTask(
                StudyTaskEntity(
                    title = title,
                    category = category,
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

    // --- Focus Timer Controls ---
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
    }

    fun setTimerConfig(mode: TimerMode, customMinutes: Int = 25) {
        stopTimer()
        val totalSec = if (mode == TimerMode.STOPWATCH) 0 else customMinutes * 60
        _timerState.value = _timerState.value.copy(
            mode = mode,
            targetSeconds = totalSec,
            elapsedSeconds = 0,
            isRunning = false,
            isPaused = false
        )
    }

    fun startTimer() {
        if (_timerState.value.isRunning) return

        _timerState.value = _timerState.value.copy(isRunning = true, isPaused = false)
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timerState.value.isRunning && !_timerState.value.isPaused) {
                delay(1000)
                val current = _timerState.value
                val newElapsed = current.elapsedSeconds + 1

                if (current.mode != TimerMode.STOPWATCH && newElapsed >= current.targetSeconds) {
                    _timerState.value = current.copy(
                        elapsedSeconds = current.targetSeconds,
                        isRunning = false,
                        isPaused = false
                    )
                    _userFeedbackMessage.value = "Session completed! Record your focus log."
                    break
                } else {
                    _timerState.value = current.copy(elapsedSeconds = newElapsed)
                }
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _timerState.value = _timerState.value.copy(isRunning = false, isPaused = true)
    }

    fun stopTimer() {
        timerJob?.cancel()
        _timerState.value = _timerState.value.copy(
            isRunning = false,
            isPaused = false,
            elapsedSeconds = 0
        )
    }

    fun saveCompletedSession(notes: String?) {
        val state = _timerState.value
        val durationMins = maxOf(1, state.elapsedSeconds / 60)
        viewModelScope.launch {
            repository.recordStudySession(
                StudySessionEntity(
                    taskId = state.selectedTaskId,
                    taskTitle = state.selectedTaskTitle,
                    category = state.selectedCategory,
                    durationMinutes = durationMins,
                    sessionType = state.mode.name,
                    date = _selectedDate.value,
                    learnedNotes = notes
                )
            )
            stopTimer()
            _userFeedbackMessage.value = "Logged $durationMins min study session"
        }
    }

    // --- Settings & Database Reset ---
    fun clearAllUserData() {
        viewModelScope.launch {
            repository.clearAllData()
            _userFeedbackMessage.value = "All data cleared and reinitialized"
        }
    }
}
