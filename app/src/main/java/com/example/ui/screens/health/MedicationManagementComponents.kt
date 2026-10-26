package com.example.ui.screens.health

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.MedicationEntity
import com.example.data.entity.MedicationIntakeEntity
import com.example.ui.components.CalmEmptyState
import com.example.ui.components.StatusBadge
import com.example.util.DateTimeUtils

/**
 * Data structure representing an individual scheduled dose slot on a given date.
 */
data class ScheduledDoseSlot(
    val medication: MedicationEntity,
    val scheduledTimeMinutes: Int,
    val intake: MedicationIntakeEntity?
)

/**
 * Comprehensive Medication Management Section with:
 * - Subtabs: "Today's Doses", "Medications & Schedules", "Intake History Log"
 * - Multi-dose scheduling per day
 * - Recurrence rules (Everyday vs selected days of week)
 * - Retrospective correction with confirmed timestamp
 * - Full preservation of historical intakes upon discontinuation/archive
 */
@Composable
fun FullMedicationCenter(
    date: String,
    medications: List<MedicationEntity>,
    archivedMedications: List<MedicationEntity>,
    intakesForDate: List<MedicationIntakeEntity>,
    historyIntakes: List<MedicationIntakeEntity>,
    distinctNames: List<String>,
    onAddOrEditSchedule: (MedicationEntity?) -> Unit,
    onRecordIntake: (MedicationEntity, Int, String, String?) -> Unit,
    onCorrectIntake: (MedicationIntakeEntity, String, String?) -> Unit,
    onArchiveMedication: (Long, Boolean) -> Unit,
    onDeleteMedication: (Long) -> Unit
) {
    var subTab by remember { mutableIntStateOf(0) }
    val tabLabels = listOf("Today's Schedule", "Medications List", "Intake History")

    var intakeToCorrect by remember { mutableStateOf<MedicationIntakeEntity?>(null) }
    var medToDelete by remember { mutableStateOf<MedicationEntity?>(null) }
    var medToArchive by remember { mutableStateOf<MedicationEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = subTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            tabLabels.forEachIndexed { index, label ->
                Tab(
                    selected = subTab == index,
                    onClick = { subTab = index },
                    text = { Text(label, style = MaterialTheme.typography.labelMedium) }
                )
            }
        }

        when (subTab) {
            0 -> TodayMedicationsSubTab(
                date = date,
                medications = medications,
                intakes = intakesForDate,
                onAddMedClick = { onAddOrEditSchedule(null) },
                onRecordIntake = onRecordIntake,
                onStartCorrection = { intakeToCorrect = it }
            )
            1 -> MedicationsListSubTab(
                activeMedications = medications,
                archivedMedications = archivedMedications,
                onAddMedClick = { onAddOrEditSchedule(null) },
                onEditMedClick = { onAddOrEditSchedule(it) },
                onArchiveClick = { medToArchive = it },
                onDeleteClick = { medToDelete = it }
            )
            2 -> IntakeHistorySubTab(
                history = historyIntakes,
                onCorrectIntake = { intakeToCorrect = it }
            )
        }
    }

    // Retrospective Correction Dialog
    if (intakeToCorrect != null) {
        MedicationCorrectionDialog(
            intake = intakeToCorrect!!,
            onDismiss = { intakeToCorrect = null },
            onConfirmCorrection = { newStatus, newNotes ->
                onCorrectIntake(intakeToCorrect!!, newStatus, newNotes)
                intakeToCorrect = null
            }
        )
    }

    // Discontinue / Archive Confirmation Dialog
    if (medToArchive != null) {
        val med = medToArchive!!
        val willArchive = !med.isArchived
        AlertDialog(
            onDismissRequest = { medToArchive = null },
            title = { Text(if (willArchive) "Discontinue Medication?" else "Restore Medication?") },
            text = {
                Text(
                    if (willArchive)
                        "Discontinuing \"${med.name}\" removes it from future scheduled daily doses. Past intake records and logs are safely preserved."
                    else
                        "Restore \"${med.name}\" back into your active daily schedule?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onArchiveMedication(med.id, willArchive)
                        medToArchive = null
                    }
                ) {
                    Text(if (willArchive) "Discontinue & Keep History" else "Restore Schedule")
                }
            },
            dismissButton = {
                TextButton(onClick = { medToArchive = null }) { Text("Cancel") }
            }
        )
    }

    // Delete Medication Confirmation Dialog
    if (medToDelete != null) {
        val med = medToDelete!!
        AlertDialog(
            onDismissRequest = { medToDelete = null },
            title = { Text("Delete Medication Schedule?") },
            text = {
                Text("Are you sure you want to delete the schedule for \"${med.name}\"?\n\nAll past recorded intake history will remain safely preserved.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMedication(med.id)
                        medToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Schedule")
                }
            },
            dismissButton = {
                TextButton(onClick = { medToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

/**
 * Tab 1: Today's Scheduled Doses across active medications.
 */
@Composable
private fun TodayMedicationsSubTab(
    date: String,
    medications: List<MedicationEntity>,
    intakes: List<MedicationIntakeEntity>,
    onAddMedClick: () -> Unit,
    onRecordIntake: (MedicationEntity, Int, String, String?) -> Unit,
    onStartCorrection: (MedicationIntakeEntity) -> Unit
) {
    val dayAbbr = DateTimeUtils.getDayOfWeekAbbreviation(date)

    // Filter medications that are active on this date and day of week
    val applicableMeds = medications.filter { med ->
        med.isEffectiveOnDate(date) && med.isScheduledForDayOfWeek(dayAbbr)
    }

    // Generate individual dose slots
    val doseSlots = applicableMeds.flatMap { med ->
        med.getScheduledTimes().map { timeMinutes ->
            val matchingIntake = intakes.firstOrNull {
                it.medicationId == med.id && it.scheduledTimeMinutes == timeMinutes
            }
            ScheduledDoseSlot(
                medication = med,
                scheduledTimeMinutes = timeMinutes,
                intake = matchingIntake
            )
        }
    }.sortedBy { it.scheduledTimeMinutes }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Medication records are stored strictly locally and privately. Dismissing a reminder never marks a dose taken.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Scheduled for Today ($dayAbbr)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${doseSlots.count { it.intake?.status == "TAKEN" }} of ${doseSlots.size} doses logged taken",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onAddMedClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_medication_schedule_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Dose")
                }
            }
        }

        if (doseSlots.isEmpty()) {
            item {
                CalmEmptyState(
                    icon = Icons.Default.Medication,
                    title = "No Medications Scheduled for Today",
                    message = "No active medication doses match today's date and day of the week ($dayAbbr).",
                    actionLabel = "Add Medication Schedule",
                    onActionClick = onAddMedClick
                )
            }
        } else {
            items(doseSlots, key = { "${it.medication.id}_${it.scheduledTimeMinutes}" }) { slot ->
                val med = slot.medication
                val intake = slot.intake
                val status = intake?.status ?: "UNRECORDED"

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth().testTag("med_slot_${med.id}_${slot.scheduledTimeMinutes}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = DateTimeUtils.formatMinutesToTime(slot.scheduledTimeMinutes),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                if (!med.dosageForm.isNullOrBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = med.dosageForm,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            StatusBadge(status = status)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "${med.name} • ${med.dosage} ${med.unit}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (med.instructions.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Instructions: ${med.instructions}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!med.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Notes: ${med.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (status == "UNRECORDED") {
                            // Unrecorded: Show Take, Skip, Delayed, Missed buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onRecordIntake(med, slot.scheduledTimeMinutes, "TAKEN", null) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1.2f).height(46.dp).testTag("take_btn_${med.id}")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Taken")
                                }

                                FilledTonalButton(
                                    onClick = { onRecordIntake(med, slot.scheduledTimeMinutes, "SKIPPED", null) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).height(46.dp)
                                ) {
                                    Text("Skip")
                                }

                                OutlinedButton(
                                    onClick = { onRecordIntake(med, slot.scheduledTimeMinutes, "DELAYED", null) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1.1f).height(46.dp)
                                ) {
                                    Text("Delayed")
                                }
                            }
                        } else {
                            // Already Recorded: show intake timestamp and retrospective correction affordance
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        val timeStr = intake?.actualActionTime?.let { DateTimeUtils.formatTimeOnly(it) } ?: ""
                                        Text(
                                            text = "$status at $timeStr",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (intake?.isCorrected == true) {
                                            Text(
                                                text = "Corrected retrospective entry",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                        if (!intake?.notes.isNullOrBlank()) {
                                            Text(
                                                text = "Note: ${intake?.notes}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            if (intake != null) onStartCorrection(intake)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("correct_intake_btn_${med.id}")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Correct", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 2: Medications & Schedules CRUD manager.
 */
@Composable
private fun MedicationsListSubTab(
    activeMedications: List<MedicationEntity>,
    archivedMedications: List<MedicationEntity>,
    onAddMedClick: () -> Unit,
    onEditMedClick: (MedicationEntity) -> Unit,
    onArchiveClick: (MedicationEntity) -> Unit,
    onDeleteClick: (MedicationEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Medications (${activeMedications.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Button(
                    onClick = onAddMedClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_new_med_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Medication")
                }
            }
        }

        if (activeMedications.isEmpty()) {
            item {
                CalmEmptyState(
                    icon = Icons.Default.Medication,
                    title = "No Active Medication Schedules",
                    message = "Tap 'New Medication' to configure dosage, scheduled times, and repeat rules.",
                    actionLabel = "Add Medication",
                    onActionClick = onAddMedClick
                )
            }
        } else {
            items(activeMedications, key = { it.id }) { med ->
                MedicationScheduleCard(
                    med = med,
                    isArchived = false,
                    onEdit = { onEditMedClick(med) },
                    onArchive = { onArchiveClick(med) },
                    onDelete = { onDeleteClick(med) }
                )
            }
        }

        if (archivedMedications.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Discontinued / Archived Medications (${archivedMedications.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(archivedMedications, key = { it.id }) { med ->
                MedicationScheduleCard(
                    med = med,
                    isArchived = true,
                    onEdit = { onEditMedClick(med) },
                    onArchive = { onArchiveClick(med) },
                    onDelete = { onDeleteClick(med) }
                )
            }
        }
    }
}

@Composable
private fun MedicationScheduleCard(
    med: MedicationEntity,
    isArchived: Boolean,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isArchived)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${med.name} • ${med.dosage} ${med.unit}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isArchived) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                    )
                    if (!med.dosageForm.isNullOrBlank()) {
                        Text(
                            text = "Form: ${med.dosageForm}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Schedule", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onArchive) {
                        Icon(
                            imageVector = if (isArchived) Icons.Default.Restore else Icons.Default.Archive,
                            contentDescription = if (isArchived) "Restore" else "Discontinue",
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Times summary
            val timesStr = med.getScheduledTimes().joinToString(", ") { DateTimeUtils.formatMinutesToTime(it) }
            Text(
                text = "Scheduled times: $timesStr",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Recurrence summary
            val repeatStr = if (med.repeatDays.equals("DAILY", ignoreCase = true)) "Every day" else "On ${med.repeatDays}"
            Text(
                text = "Recurrence: $repeatStr",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (med.instructions.isNotBlank()) {
                Text(
                    text = "Instructions: ${med.instructions}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Tab 3: Complete Intake History Log with Retrospective Correction capability.
 */
@Composable
private fun IntakeHistorySubTab(
    history: List<MedicationIntakeEntity>,
    onCorrectIntake: (MedicationIntakeEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Intake logs track actual timestamps and statuses. Past mistaken records can be corrected with a permanent confirmation timestamp.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (history.isEmpty()) {
            item {
                CalmEmptyState(
                    icon = Icons.Default.History,
                    title = "No Intake History Logged",
                    message = "When you log doses as Taken, Skipped, Delayed, or Missed, they will appear here chronologically.",
                    actionLabel = null
                )
            }
        } else {
            items(history, key = { it.id }) { record ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = record.date,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Scheduled: ${DateTimeUtils.formatMinutesToTime(record.scheduledTimeMinutes)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "${record.medicationName.ifBlank { "Medication #${record.medicationId}" }} ${record.dosageSnapshot}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (record.actualActionTime != null) {
                                Text(
                                    text = "Recorded at ${DateTimeUtils.formatTimestamp(record.actualActionTime)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (record.isCorrected) {
                                Text(
                                    text = "✓ Retrospectively Corrected",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }

                            if (!record.notes.isNullOrBlank()) {
                                Text(
                                    text = "Notes: ${record.notes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            StatusBadge(status = record.status)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = { onCorrectIntake(record) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Correct", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Retrospective Correction Dialog:
 * Allows user to correct an accidental or mistaken status with updated timestamp confirmation.
 */
@Composable
fun MedicationCorrectionDialog(
    intake: MedicationIntakeEntity,
    onDismiss: () -> Unit,
    onConfirmCorrection: (String, String?) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(intake.status) }
    var notes by remember { mutableStateOf(intake.notes ?: "") }

    val statusOptions = listOf("TAKEN", "SKIPPED", "DELAYED", "MISSED")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Correct Past Intake Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "${intake.medicationName.ifBlank { "Medication" }} (${DateTimeUtils.formatMinutesToTime(intake.scheduledTimeMinutes)}) on ${intake.date}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                Text(
                    text = "Select corrected status:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    statusOptions.forEach { status ->
                        val isSelected = selectedStatus == status
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedStatus = status }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = status.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Reason for correction / Notes") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Confirmation: This retrospective update will record a new timestamp marking the entry as corrected. Historical records are preserved.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmCorrection(selectedStatus, notes.ifBlank { null }) }
            ) {
                Text("Confirm Correction")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Advanced Medication Schedule Dialog:
 * Supports:
 * - Autocomplete / quick chips from previously used medication names
 * - Dose amount + configurable unit (mg, mcg, g, mL, tablet, capsule, drops, other)
 * - Dosage form chips (Tablet, Capsule, Liquid, Inhaler, Drops, Injection, Patch, Topical, Other)
 * - Prescribed instructions with presets
 * - One or more scheduled times per day
 * - Recurrence: Everyday or specific days of week
 * - Start date and optional end date
 * - Reminder toggle & notes
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicationScheduleDialog(
    existing: MedicationEntity?,
    distinctNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        name: String,
        dosage: String,
        unit: String,
        instructions: String,
        scheduledTimes: List<Int>,
        dosageForm: String?,
        repeatDays: String,
        startDate: String,
        endDate: String?,
        reminder: Boolean,
        notes: String?
    ) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var dose by remember { mutableStateOf(existing?.dosage ?: "") }
    var unit by remember { mutableStateOf(existing?.unit ?: "mg") }
    var dosageForm by remember { mutableStateOf(existing?.dosageForm ?: "Tablet") }
    var instructions by remember { mutableStateOf(existing?.instructions ?: "Take with food") }

    val initialTimes = existing?.getScheduledTimes() ?: listOf(480) // 8:00 AM
    val scheduledTimes = remember { mutableStateListOf<Int>().apply { addAll(initialTimes) } }

    var isEveryday by remember { mutableStateOf(existing?.repeatDays?.equals("DAILY", ignoreCase = true) ?: true) }
    val selectedDays = remember {
        mutableStateListOf<String>().apply {
            val days = existing?.repeatDays?.split(",")?.map { it.trim().uppercase() } ?: emptyList()
            if (!isEveryday && days.isNotEmpty()) addAll(days)
        }
    }

    var startDate by remember { mutableStateOf(existing?.startDate?.ifBlank { DateTimeUtils.todayDateString() } ?: DateTimeUtils.todayDateString()) }
    var endDate by remember { mutableStateOf(existing?.endDate ?: "") }
    var reminderEnabled by remember { mutableStateOf(existing?.reminderEnabled ?: false) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }

    val unitOptions = listOf("mg", "mcg", "g", "mL", "tablet", "capsule", "drops", "puff", "other")
    val formOptions = listOf("Tablet", "Capsule", "Liquid", "Inhaler", "Drops", "Injection", "Patch", "Topical", "Other")
    val instructionPresets = listOf("Take with food", "Take before meals", "Take on empty stomach", "Take at bedtime", "With full glass of water")
    val weekDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "Edit Medication Schedule" else "Add Medication Schedule") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Medication Name *") },
                    placeholder = { Text("e.g. Sertraline, Vitamin D3") },
                    modifier = Modifier.fillMaxWidth().testTag("med_name_input")
                )

                // Distinct Name Suggestions
                if (distinctNames.isNotEmpty() && name.isBlank()) {
                    Text(
                        text = "Previously entered:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        distinctNames.take(6).forEach { prevName ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { name = prevName }
                            ) {
                                Text(
                                    text = prevName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Dose Amount & Unit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dose,
                        onValueChange = { dose = it },
                        label = { Text("Dose Amount *") },
                        placeholder = { Text("50") },
                        modifier = Modifier.weight(1f).testTag("med_dose_input")
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        modifier = Modifier.weight(1f).testTag("med_unit_input")
                    )
                }

                // Quick Unit chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    unitOptions.forEach { opt ->
                        FilterChip(
                            selected = unit.equals(opt, ignoreCase = true),
                            onClick = { unit = opt },
                            label = { Text(opt, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Dosage Form
                Text(
                    text = "Dosage Form:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    formOptions.forEach { form ->
                        FilterChip(
                            selected = dosageForm == form,
                            onClick = { dosageForm = form },
                            label = { Text(form, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Instructions
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Prescribed Instructions") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick instruction chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    instructionPresets.forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.clickable { instructions = preset }
                        ) {
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Scheduled Times (One or More Times Per Day)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scheduled Times (${scheduledTimes.size}/day)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = {
                            // Add a default next time (e.g. evening or +4 hours)
                            val nextTime = if (scheduledTimes.isEmpty()) 480 else (scheduledTimes.last() + 360) % 1440
                            scheduledTimes.add(nextTime)
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Add Time")
                    }
                }

                scheduledTimes.forEachIndexed { index, timeMinutes ->
                    var currentMinutes by remember(timeMinutes) { mutableIntStateOf(timeMinutes) }
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Dose #${index + 1}: ${DateTimeUtils.formatMinutesToTime(currentMinutes)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (scheduledTimes.size > 1) {
                                    IconButton(
                                        onClick = { scheduledTimes.removeAt(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove Time", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Slider(
                                value = (currentMinutes / 15).toFloat(),
                                onValueChange = {
                                    val newMin = (it.toInt() * 15) % 1440
                                    currentMinutes = newMin
                                    scheduledTimes[index] = newMin
                                },
                                valueRange = 0f..95f,
                                steps = 95
                            )
                        }
                    }
                }

                // Repeat Schedule
                Text(
                    text = "Repeat Schedule:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isEveryday,
                        onClick = { isEveryday = true },
                        label = { Text("Every Day") }
                    )
                    FilterChip(
                        selected = !isEveryday,
                        onClick = {
                            isEveryday = false
                            if (selectedDays.isEmpty()) selectedDays.addAll(listOf("MON", "WED", "FRI"))
                        },
                        label = { Text("Selected Days") }
                    )
                }

                if (!isEveryday) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        weekDays.forEach { day ->
                            val isDaySelected = selectedDays.contains(day)
                            FilterChip(
                                selected = isDaySelected,
                                onClick = {
                                    if (isDaySelected) selectedDays.remove(day) else selectedDays.add(day)
                                },
                                label = { Text(day, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // Start & End Dates
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("Start Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("End Date (Optional)") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Reminder Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Reminder Notifications",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Send reminder at scheduled dose times",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it }
                    )
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Doctor / Pharmacy / Additional Notes") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Safety Disclaimer
                Text(
                    text = "Rhythm & Focus stores all medication data exclusively on this device. Dosages are never automatically modified or recommended.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && dose.isNotBlank()) {
                        val repeatDaysStr = if (isEveryday || selectedDays.isEmpty()) "DAILY" else selectedDays.joinToString(",")
                        onSave(
                            existing?.id ?: 0L,
                            name,
                            dose,
                            unit,
                            instructions,
                            scheduledTimes.toList(),
                            dosageForm,
                            repeatDaysStr,
                            startDate,
                            endDate.ifBlank { null },
                            reminderEnabled,
                            notes.ifBlank { null }
                        )
                    }
                },
                enabled = name.isNotBlank() && dose.isNotBlank(),
                modifier = Modifier.testTag("save_medication_schedule_submit")
            ) {
                Text(if (existing != null) "Update Schedule" else "Save Medication")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
