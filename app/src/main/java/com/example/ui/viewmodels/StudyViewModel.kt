package com.example.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.ActiveTimerDraftEntity
import com.example.data.entity.StudyCategoryEntity
import com.example.data.entity.StudySessionEntity
import com.example.data.entity.StudyTaskEntity
import com.example.data.model.BreakPoint
import com.example.data.model.StudyProtocol
import com.example.data.model.TimerPhase
import com.example.data.repository.AppRepository
import com.example.ui.TimerMode
import com.example.ui.TimerState
import com.example.util.DateTimeUtils
import com.example.util.FocusAudioLifecycleManager
import com.example.util.FocusWallpaperManager
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StudyViewModel(
    private val repository: AppRepository,
    private val context: Context? = null
) : ViewModel() {

    private val appContext: Context? get() = (context ?: com.example.data.database.AppDatabase.appContext)?.applicationContext

    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage: StateFlow<String?> = _userFeedbackMessage.asStateFlow()

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

    // Immersive Fullscreen Focus Mode State
    private val _isStudyFocusModeActive = MutableStateFlow(false)
    val isStudyFocusModeActive: StateFlow<Boolean> = _isStudyFocusModeActive.asStateFlow()

    fun setStudyFocusMode(active: Boolean) {
        _isStudyFocusModeActive.value = active
    }

    fun clearFeedbackMessage() {
        _userFeedbackMessage.value = null
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

    fun deleteStudyCategory(id: Long, categoryName: String, fallbackName: String = "General") {
        viewModelScope.launch {
            repository.deleteCategoryAndReassignTasks(id, categoryName, fallbackName)
            _userFeedbackMessage.value = "Category deleted, tasks reassigned to '$fallbackName'"
        }
    }

    // --- Study Task Actions ---
    fun createStudyTask(
        title: String,
        category: String,
        estimatedMinutes: Int,
        isPriority: Boolean,
        dateCreated: String = DateTimeUtils.todayDateString()
    ) {
        viewModelScope.launch {
            val id = repository.saveStudyTask(
                StudyTaskEntity(
                    title = title.trim(),
                    category = category.trim().ifBlank { "General" },
                    estimatedMinutes = estimatedMinutes,
                    isDailyPriority = isPriority,
                    dateCreated = dateCreated
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

    fun updateTaskStatus(task: StudyTaskEntity, newStatus: String, selectedDate: String = DateTimeUtils.todayDateString()) {
        viewModelScope.launch {
            val completedDate = if (newStatus == "COMPLETED") selectedDate else null
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

    // --- Timer Actions ---
    fun setTimerConfig(mode: TimerMode, durationMinutes: Int) {
        val targetSeconds = durationMinutes * 60
        _timerState.value = _timerState.value.copy(
            mode = mode,
            targetSeconds = targetSeconds,
            remainingSeconds = targetSeconds,
            elapsedSeconds = 0,
            accumulatedElapsedSeconds = 0,
            isRunning = false,
            isPaused = false,
            isCompleted = false
        )
    }

    fun setSelectedTaskForTimer(taskId: Long?, title: String, category: String) {
        _timerState.value = _timerState.value.copy(
            selectedTaskId = taskId,
            selectedTaskTitle = title,
            selectedCategory = category
        )
    }

    suspend fun restoreTimerStateFromDraft() {
        val draft = repository.getTimerDraftSync() ?: return
        val mode = try {
            TimerMode.valueOf(draft.mode)
        } catch (_: Exception) {
            TimerMode.POMODORO
        }

        val now = System.currentTimeMillis()
        val targetSec = draft.targetSeconds
        var accumulated = draft.accumulatedElapsedSeconds

        if (draft.isRunning && !draft.isPaused) {
            val wallClockElapsed = ((now - draft.startWallClockMillis) / 1000L).toInt()
            accumulated += wallClockElapsed.coerceAtLeast(0)
        }

        val elapsed = accumulated.coerceAtMost(targetSec)
        val remaining = (targetSec - elapsed).coerceAtLeast(0)
        val completed = targetSec > 0 && elapsed >= targetSec

        _timerState.value = _timerState.value.copy(
            isRunning = draft.isRunning && !completed,
            isPaused = draft.isPaused,
            isCompleted = completed,
            mode = mode,
            targetSeconds = targetSec,
            elapsedSeconds = elapsed,
            remainingSeconds = remaining,
            startWallClockMillis = if (draft.isRunning && !draft.isPaused) now else 0L,
            accumulatedElapsedSeconds = accumulated,
            sessionStartTimestamp = draft.startWallClockMillis,
            selectedTaskId = draft.taskId,
            selectedTaskTitle = draft.taskTitle,
            selectedCategory = draft.category
        )
    }

    fun resetTimer() {
        timerTickerJob?.cancel()
        timerTickerJob = null
        val targetSec = _timerState.value.mode.defaultMinutes * 60
        _timerState.value = _timerState.value.copy(
            isRunning = false,
            isPaused = false,
            isCompleted = false,
            elapsedSeconds = 0,
            remainingSeconds = targetSec,
            targetSeconds = targetSec,
            accumulatedElapsedSeconds = 0,
            startWallClockMillis = 0L,
            sessionStartTimestamp = 0L
        )
        viewModelScope.launch {
            repository.clearTimerDraft()
        }
    }
}

