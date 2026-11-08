package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.RoutineAnchorEntity
import com.example.data.entity.RoutineEntryEntity
import com.example.data.repository.AppRepository
import com.example.util.DateTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RoutineViewModel(
    private val repository: AppRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(DateTimeUtils.todayDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage: StateFlow<String?> = _userFeedbackMessage.asStateFlow()

    val routineAnchors: StateFlow<List<RoutineAnchorEntity>> = repository.routineAnchors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val routineEntriesForDate: StateFlow<List<RoutineEntryEntity>> = _selectedDate
        .flatMapLatest { date -> repository.getRoutineEntriesForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    fun clearFeedbackMessage() {
        _userFeedbackMessage.value = null
    }

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

    fun updateRoutineAnchorTarget(anchor: RoutineAnchorEntity, targetMinutes: Int) {
        if (targetMinutes !in 0..1439) {
            _userFeedbackMessage.value = "Choose a valid target time"
            return
        }
        viewModelScope.launch {
            repository.updateAnchor(anchor.copy(defaultTargetMinutes = targetMinutes))
            _userFeedbackMessage.value = "${anchor.name} target updated to ${DateTimeUtils.formatMinutesToTime(targetMinutes)}"
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

    suspend fun markRoutineDoneNowSync(anchor: RoutineAnchorEntity, nowMinutes: Int = DateTimeUtils.currentMinutesOfDay()) {
        repository.saveRoutineEntry(
            RoutineEntryEntity(
                date = _selectedDate.value,
                anchorId = anchor.id,
                targetTimeMinutes = anchor.defaultTargetMinutes,
                actualTimeMinutes = nowMinutes,
                status = "COMPLETED"
            )
        )
    }
}

