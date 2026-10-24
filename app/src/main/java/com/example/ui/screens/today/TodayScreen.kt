package com.example.ui.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.HealthCheckinEntity
import com.example.data.entity.MedicationEntity
import com.example.data.entity.MedicationIntakeEntity
import com.example.data.entity.RoutineAnchorEntity
import com.example.data.entity.RoutineEntryEntity
import com.example.data.entity.SleepEntryEntity
import com.example.data.entity.StudyTaskEntity
import com.example.ui.AppDestination
import com.example.ui.MainViewModel
import com.example.ui.components.CalmEmptyState
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.util.DateTimeUtils

@Composable
fun TodayScreen(
    viewModel: MainViewModel,
    isExpandedScreen: Boolean,
    modifier: Modifier = Modifier
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val routineAnchors by viewModel.routineAnchors.collectAsStateWithLifecycle()
    val routineEntries by viewModel.routineEntriesForDate.collectAsStateWithLifecycle()
    val sleepEntry by viewModel.sleepForDate.collectAsStateWithLifecycle()
    val healthCheckin by viewModel.healthForDate.collectAsStateWithLifecycle()
    val waterEntries by viewModel.waterEntriesForDate.collectAsStateWithLifecycle()
    val activeMedications by viewModel.activeMedications.collectAsStateWithLifecycle()
    val medicationIntakes by viewModel.medicationIntakesForDate.collectAsStateWithLifecycle()
    val dailyPriorityTask by viewModel.dailyPriorityTask.collectAsStateWithLifecycle()

    val totalWaterMl = waterEntries.sumOf { it.volumeMl }
    val completedRoutines = routineEntries.count { it.status == "COMPLETED" }

    if (isExpandedScreen) {
        // Tablet / Wide 2-Column Dashboard (e.g. Xiaomi Pad 7)
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    TodayGreetingHeader(selectedDate = selectedDate)
                }
                item {
                    RoutineSummaryCard(
                        completedCount = completedRoutines,
                        totalCount = routineAnchors.size,
                        anchors = routineAnchors,
                        entries = routineEntries,
                        onDoneNow = { anchor -> viewModel.markRoutineDoneNow(anchor) },
                        onViewRoutine = { viewModel.navigateTo(AppDestination.ROUTINE) }
                    )
                }
                item {
                    SleepSummaryCard(
                        sleep = sleepEntry,
                        onLogSleep = { viewModel.navigateTo(AppDestination.HEALTH) }
                    )
                }
                item {
                    MoodEnergySummaryCard(
                        health = healthCheckin,
                        onLogHealth = { viewModel.navigateTo(AppDestination.HEALTH) }
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    DailyPriorityStudyCard(
                        task = dailyPriorityTask,
                        onStartFocus = { task ->
                            viewModel.selectTaskForTimer(task)
                            viewModel.navigateTo(AppDestination.STUDY)
                        },
                        onViewTasks = { viewModel.navigateTo(AppDestination.STUDY) }
                    )
                }
                item {
                    WaterTrackingCard(
                        totalMl = totalWaterMl,
                        targetMl = 2000,
                        onAddWater = { amount -> viewModel.addWater(amount) },
                        onViewWater = { viewModel.navigateTo(AppDestination.HEALTH) }
                    )
                }
                item {
                    MedicationOverviewCard(
                        medications = activeMedications,
                        intakes = medicationIntakes,
                        onRecordIntake = { med, status ->
                            viewModel.recordMedicationIntake(med, status, null)
                        },
                        onManageMedications = { viewModel.navigateTo(AppDestination.HEALTH) }
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    } else {
        // Phone / Compact Single Column
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                TodayGreetingHeader(selectedDate = selectedDate)
            }
            item {
                RoutineSummaryCard(
                    completedCount = completedRoutines,
                    totalCount = routineAnchors.size,
                    anchors = routineAnchors,
                    entries = routineEntries,
                    onDoneNow = { anchor -> viewModel.markRoutineDoneNow(anchor) },
                    onViewRoutine = { viewModel.navigateTo(AppDestination.ROUTINE) }
                )
            }
            item {
                DailyPriorityStudyCard(
                    task = dailyPriorityTask,
                    onStartFocus = { task ->
                        viewModel.selectTaskForTimer(task)
                        viewModel.navigateTo(AppDestination.STUDY)
                    },
                    onViewTasks = { viewModel.navigateTo(AppDestination.STUDY) }
                )
            }
            item {
                SleepSummaryCard(
                    sleep = sleepEntry,
                    onLogSleep = { viewModel.navigateTo(AppDestination.HEALTH) }
                )
            }
            item {
                MoodEnergySummaryCard(
                    health = healthCheckin,
                    onLogHealth = { viewModel.navigateTo(AppDestination.HEALTH) }
                )
            }
            item {
                WaterTrackingCard(
                    totalMl = totalWaterMl,
                    targetMl = 2000,
                    onAddWater = { amount -> viewModel.addWater(amount) },
                    onViewWater = { viewModel.navigateTo(AppDestination.HEALTH) }
                )
            }
            item {
                MedicationOverviewCard(
                    medications = activeMedications,
                    intakes = medicationIntakes,
                    onRecordIntake = { med, status ->
                        viewModel.recordMedicationIntake(med, status, null)
                    },
                    onManageMedications = { viewModel.navigateTo(AppDestination.HEALTH) }
                )
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun TodayGreetingHeader(selectedDate: String) {
    val greeting = DateTimeUtils.greetingForCurrentTime()
    val displayDate = DateTimeUtils.formatDisplayDate(selectedDate)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = displayDate,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RoutineSummaryCard(
    completedCount: Int,
    totalCount: Int,
    anchors: List<RoutineAnchorEntity>,
    entries: List<RoutineEntryEntity>,
    onDoneNow: (RoutineAnchorEntity) -> Unit,
    onViewRoutine: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("today_routine_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Daily Routine Anchors",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "$completedCount / $totalCount recorded",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = {
                    if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Show next upcoming or unrecorded anchor
            val nextUnrecorded = anchors.firstOrNull { anchor ->
                val entry = entries.firstOrNull { it.anchorId == anchor.id }
                entry == null || entry.status == "UNRECORDED"
            }

            if (nextUnrecorded != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Next Anchor: ${nextUnrecorded.name}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Target: ${DateTimeUtils.formatMinutesToTime(nextUnrecorded.defaultTargetMinutes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { onDoneNow(nextUnrecorded) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("quick_done_anchor_button")
                        ) {
                            Text("Done Now", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            } else {
                Text(
                    text = "All routine anchors recorded for today.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onViewRoutine,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("view_all_routines_button")
            ) {
                Text("View All Routine Anchors")
            }
        }
    }
}

@Composable
fun DailyPriorityStudyCard(
    task: StudyTaskEntity?,
    onStartFocus: (StudyTaskEntity?) -> Unit,
    onViewTasks: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("today_study_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Main Study Priority",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (task != null) {
                    StatusBadge(status = task.status)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (task != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = task.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "${task.estimatedMinutes} min target",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (!task.nextSmallStep.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Next step: ${task.nextSmallStep}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onStartFocus(task) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("start_priority_focus_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Focus Session")
                    }
                }
            } else {
                CalmEmptyState(
                    icon = Icons.Default.Psychology,
                    title = "No Daily Priority Set",
                    message = "Choose one main study or project priority for today to keep focus gentle and realistic.",
                    actionLabel = "Choose Study Priority",
                    onActionClick = onViewTasks
                )
            }
        }
    }
}

@Composable
fun SleepSummaryCard(
    sleep: SleepEntryEntity?,
    onLogSleep: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("today_sleep_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Sleep Summary",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                OutlinedButton(
                    onClick = onLogSleep,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("edit_sleep_button")
                ) {
                    Text(if (sleep != null) "Edit" else "Log Sleep")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (sleep != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = DateTimeUtils.formatDurationMinutes(sleep.durationMinutes),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${DateTimeUtils.formatMinutesToTime(sleep.bedtimeMinutes)} → ${DateTimeUtils.formatMinutesToTime(sleep.wakeTimeMinutes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Quality Rating",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row {
                                for (i in 1..5) {
                                    Icon(
                                        imageVector = if (i <= sleep.qualityRating) Icons.Default.Star else Icons.Outlined.StarOutline,
                                        contentDescription = null,
                                        tint = if (i <= sleep.qualityRating) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                CalmEmptyState(
                    icon = Icons.Default.Bedtime,
                    title = "Sleep Not Recorded Yet",
                    message = "Record last night's bedtime and wake time to keep your circadian rhythm in balance.",
                    actionLabel = "Log Sleep",
                    onActionClick = onLogSleep
                )
            }
        }
    }
}

@Composable
fun MoodEnergySummaryCard(
    health: HealthCheckinEntity?,
    onLogHealth: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("today_health_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SelfImprovement,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Mood & Energy State",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                OutlinedButton(
                    onClick = onLogHealth,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("check_in_button")
                ) {
                    Text(if (health != null) "Update" else "Check In")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (health != null && (health.mood != null || health.energy != null)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        MetricRatingIndicator(
                            label = "Mood",
                            value = health.mood,
                            labels = listOf("Very Low", "Low", "Neutral", "Good", "Great")
                        )
                        MetricRatingIndicator(
                            label = "Energy",
                            value = health.energy,
                            labels = listOf("Exhausted", "Low", "Moderate", "High", "Very High")
                        )
                        MetricRatingIndicator(
                            label = "Stress",
                            value = health.stress,
                            labels = listOf("None", "Mild", "Moderate", "High", "Severe")
                        )
                    }
                }
            } else {
                CalmEmptyState(
                    icon = Icons.Default.SelfImprovement,
                    title = "No Check-In Recorded",
                    message = "Take 10 seconds to log your mood, energy, and stress. Measurements remain independent without guilt scores.",
                    actionLabel = "Record Check-In",
                    onActionClick = onLogHealth
                )
            }
        }
    }
}

@Composable
fun MetricRatingIndicator(label: String, value: Int?, labels: List<String>) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (value != null && value in 1..labels.size) labels[value - 1] else "—",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (value != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
        if (value != null) {
            Text(
                text = "$value/5",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WaterTrackingCard(
    totalMl: Int,
    targetMl: Int,
    onAddWater: (Int) -> Unit,
    onViewWater: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("today_water_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalDrink,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Hydration Tracker",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "$totalMl / $targetMl ml",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = {
                    (totalMl.toFloat() / targetMl.toFloat()).coerceIn(0f, 1f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = { onAddWater(250) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_water_250_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+250 ml")
                }

                FilledTonalButton(
                    onClick = { onAddWater(500) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_water_500_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+500 ml")
                }
            }
        }
    }
}

@Composable
fun MedicationOverviewCard(
    medications: List<MedicationEntity>,
    intakes: List<MedicationIntakeEntity>,
    onRecordIntake: (MedicationEntity, String) -> Unit,
    onManageMedications: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("today_medication_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Medications Schedule",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                OutlinedButton(
                    onClick = onManageMedications,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("manage_meds_button")
                ) {
                    Text("Manage")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (medications.isEmpty()) {
                CalmEmptyState(
                    icon = Icons.Default.Medication,
                    title = "No Medications Added",
                    message = "Optional medication tracking helps you log intake timestamps privately on this device.",
                    actionLabel = "Add Medication",
                    onActionClick = onManageMedications
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    medications.take(3).forEach { med ->
                        val intake = intakes.firstOrNull { it.medicationId == med.id }
                        val status = intake?.status ?: "UNRECORDED"

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${med.name} (${med.dosage} ${med.unit})",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Scheduled: ${DateTimeUtils.formatMinutesToTime(med.scheduledTimeMinutes)} • ${med.instructions}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (status == "UNRECORDED") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = { onRecordIntake(med, "TAKEN") },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Mark Taken",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(
                                            onClick = { onRecordIntake(med, "SKIPPED") },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Mark Skipped",
                                                tint = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                } else {
                                    StatusBadge(status = status)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
