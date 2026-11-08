package com.example.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.HealthCheckinEntity
import com.example.data.entity.MedicationEntity
import com.example.data.entity.MedicationIntakeEntity
import com.example.data.entity.SleepEntryEntity
import com.example.data.entity.WaterEntryEntity
import com.example.data.repository.AppRepository
import com.example.health.HealthConnectManager
import com.example.util.DateTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HealthViewModel(
    private val repository: AppRepository,
    private val context: Context? = null
) : ViewModel() {

    private val appContext: Context? get() = (context ?: com.example.data.database.AppDatabase.appContext)?.applicationContext

    private val _selectedDate = MutableStateFlow(DateTimeUtils.todayDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage: StateFlow<String?> = _userFeedbackMessage.asStateFlow()

    val healthConnectManager: HealthConnectManager? by lazy {
        appContext?.let { HealthConnectManager(it, repository) }
    }

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

    val archivedMedications: StateFlow<List<MedicationEntity>> = repository.archivedMedications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val medicationIntakesForDate: StateFlow<List<MedicationIntakeEntity>> = _selectedDate
        .flatMapLatest { date -> repository.getMedicationIntakesForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentMedicationIntakes: StateFlow<List<MedicationIntakeEntity>> = repository.getRecentIntakeHistory(100)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val distinctMedicationNames: StateFlow<List<String>> = repository.distinctMedicationNames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    fun clearFeedbackMessage() {
        _userFeedbackMessage.value = null
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

    fun deleteMedication(id: Long, ctx: Context? = appContext) {
        viewModelScope.launch {
            repository.deleteMedication(id, ctx)
            _userFeedbackMessage.value = "Medication schedule deleted (past intakes preserved)"
        }
    }
}

