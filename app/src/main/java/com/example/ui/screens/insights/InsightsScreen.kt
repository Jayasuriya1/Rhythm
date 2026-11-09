package com.example.ui.screens.insights

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.insights.ConsistencyTier
import com.example.domain.insights.FocusMetricSummary
import com.example.domain.insights.MedicationAdherenceSummary
import com.example.domain.insights.MetricCalculator
import com.example.domain.insights.MetricTrend
import com.example.domain.insights.SleepMetricSummary
import com.example.domain.insights.SrmMetricSummary
import com.example.domain.insights.WellnessMetricSummary
import com.example.ui.MainViewModel
import com.example.util.DateTimeUtils
import com.example.util.ReportExportManager
import java.util.Locale

@Composable
fun InsightsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedDaysCount by rememberSaveable { mutableIntStateOf(7) }

    val periodDates = remember(selectedDaysCount) { DateTimeUtils.getPastDays(selectedDaysCount) }
    val startDate = periodDates.firstOrNull() ?: DateTimeUtils.todayDateString()
    val endDate = periodDates.lastOrNull() ?: DateTimeUtils.todayDateString()

    // Reactive period-bounded data flows
    val sleepEntries by viewModel.getSleepBetween(startDate, endDate).collectAsStateWithLifecycle(initialValue = emptyList())
    val routineEntries by viewModel.getRoutineEntriesBetween(startDate, endDate).collectAsStateWithLifecycle(initialValue = emptyList())
    val routineAnchors by viewModel.routineAnchors.collectAsStateWithLifecycle()
    val healthEntries by viewModel.getHealthBetween(startDate, endDate).collectAsStateWithLifecycle(initialValue = emptyList())
    val medicationIntakes by viewModel.getMedicationIntakesBetween(startDate, endDate).collectAsStateWithLifecycle(initialValue = emptyList())
    val allMedications by viewModel.allMedications.collectAsStateWithLifecycle()
    val studySessions by viewModel.getStudySessionsBetween(startDate, endDate).collectAsStateWithLifecycle(initialValue = emptyList())
    val brainDumpNotes by viewModel.activeBrainDumpNotes.collectAsStateWithLifecycle()

    // Metric Calculations
    val sleepSummary = remember(periodDates, sleepEntries) {
        MetricCalculator.calculateSleepMetrics(periodDates, sleepEntries)
    }
    val srmSummary = remember(periodDates, routineAnchors, routineEntries) {
        MetricCalculator.calculateSrmMetrics(periodDates, routineAnchors, routineEntries)
    }
    val medicationSummary = remember(periodDates, allMedications, medicationIntakes) {
        MetricCalculator.calculateMedicationAdherence(periodDates, allMedications, medicationIntakes)
    }
    val wellnessSummary = remember(periodDates, healthEntries) {
        MetricCalculator.calculateWellnessMetrics(periodDates, healthEntries)
    }
    val focusSummary = remember(periodDates, studySessions) {
        MetricCalculator.calculateFocusMetrics(periodDates, studySessions)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Period Selector
        item {
            PeriodSelectorRow(
                selectedDays = selectedDaysCount,
                onSelectDays = { selectedDaysCount = it }
            )
        }

        // Overview Header Card
        item {
            ObservationalOverviewCard(
                daysCount = selectedDaysCount,
                sleepRecordedCount = sleepSummary.recordedDaysCount,
                srmRecordedEvents = srmSummary.totalRecordedEvents,
                srmExpectedEvents = srmSummary.totalExpectedEvents,
                wellnessRecordedCount = wellnessSummary.recordedDaysCount
            )
        }

        // Sleep Timing & Consistency Card
        item {
            SleepTimingCard(
                periodDates = periodDates,
                summary = sleepSummary
            )
        }

        // Social Rhythm Metric (SRM-5) Card
        item {
            SrmRegularityCard(
                summary = srmSummary,
                periodDaysCount = selectedDaysCount
            )
        }

        // Medication Adherence Card
        item {
            MedicationAdherenceCard(
                summary = medicationSummary
            )
        }

        // Mood & Energy Trends Card
        item {
            MoodEnergyTrendsCard(
                summary = wellnessSummary,
                sleepSummary = sleepSummary
            )
        }

        // Focus & Deep Work Habits Card
        item {
            FocusHabitsCard(
                summary = focusSummary
            )
        }

        // Personal Summary Export Card
        item {
            PersonalSummaryExportCard(
                startDate = startDate,
                endDate = endDate,
                sleepSummary = sleepSummary,
                srmSummary = srmSummary,
                medicationSummary = medicationSummary,
                wellnessSummary = wellnessSummary,
                focusSummary = focusSummary,
                healthCheckins = healthEntries,
                routineEntries = routineEntries,
                brainDumpNotes = brainDumpNotes,
                onCopyToClipboard = { text ->
                    val clip = ClipData.newPlainText("RhythmSummary", text)
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                    Toast.makeText(context, "Summary copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun PeriodSelectorRow(
    selectedDays: Int,
    onSelectDays: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("insights_period_selector"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = selectedDays == 7,
            onClick = { onSelectDays(7) },
            label = { Text("Past 7 Days") },
            leadingIcon = if (selectedDays == 7) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null,
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.testTag("insights_period_7_days")
        )

        FilterChip(
            selected = selectedDays == 30,
            onClick = { onSelectDays(30) },
            label = { Text("Past 30 Days") },
            leadingIcon = if (selectedDays == 30) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null,
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.testTag("insights_period_30_days")
        )
    }
}

@Composable
fun ObservationalOverviewCard(
    daysCount: Int,
    sleepRecordedCount: Int,
    srmRecordedEvents: Int,
    srmExpectedEvents: Int,
    wellnessRecordedCount: Int
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.40f)),
        modifier = Modifier.fillMaxWidth().testTag("insights_overview_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Observational Rhythm Trends ($daysCount Days)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Records are based strictly on your logged entries. Missing days indicate unrecorded intervals, not negative behavior or lost streaks.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Sleep", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$sleepRecordedCount / $daysCount d", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SRM Events", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$srmRecordedEvents / $srmExpectedEvents", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Wellness", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$wellnessRecordedCount / $daysCount d", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun SleepTimingCard(
    periodDates: List<String>,
    summary: SleepMetricSummary
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth().testTag("insights_sleep_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Sleep Timing & Consistency",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (summary.averageDurationMinutes != null) {
                    Text(
                        text = "Avg: ${DateTimeUtils.formatDurationMinutes(summary.averageDurationMinutes)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Consistency Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (summary.bedtimeConsistencyMinutes != null) {
                    val tierLabel = summary.bedtimeConsistencyTier.name.lowercase().replaceFirstChar { it.uppercase() }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "Bedtime: ±${summary.bedtimeConsistencyMinutes.toInt()}m ($tierLabel)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (summary.wakeTimeConsistencyMinutes != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "Wake: ±${summary.wakeTimeConsistencyMinutes.toInt()}m variation",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sleep Duration Bars (Sample last 7 days for clean layout)
            val displayDates = if (periodDates.size > 7) periodDates.takeLast(7) else periodDates

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                displayDates.forEach { dayStr ->
                    val duration = summary.dailyDurations[dayStr]
                    val maxTarget = 9 * 60f
                    val heightRatio = if (duration != null) (duration / maxTarget).coerceIn(0.12f, 1f) else 0.08f

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                contentDescription = if (duration != null) {
                                    "${DateTimeUtils.formatDisplayDate(dayStr)}: ${duration / 60} hours ${duration % 60} minutes sleep"
                                } else {
                                    "${DateTimeUtils.formatDisplayDate(dayStr)}: Unrecorded"
                                }
                            }
                    ) {
                        Text(
                            text = if (duration != null) "${duration / 60}h" else "—",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (duration != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height((76 * heightRatio).dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (duration != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = DateTimeUtils.formatShortDay(dayStr),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SrmRegularityCard(
    summary: SrmMetricSummary,
    periodDaysCount: Int
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth().testTag("insights_srm_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(
                            text = "Social Rhythm Metric (SRM-5)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$periodDaysCount-day routine stability window",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (summary.regularHitsRatio != null) {
                    val pct = (summary.regularHitsRatio * 100).toInt()
                    Text(
                        text = "$pct% Regular",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Anchors occurring within 45 minutes of target time support biological circadian stability.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Regular Anchor Events",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${summary.totalRegularHits} of ${summary.totalRecordedEvents} recorded",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    val progress = if (summary.totalExpectedEvents > 0) {
                        summary.totalRegularHits.toFloat() / summary.totalExpectedEvents.toFloat()
                    } else 0f

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    val unrecorded = summary.totalExpectedEvents - summary.totalRecordedEvents
                    if (unrecorded > 0) {
                        Text(
                            text = "$unrecorded routine anchor events unrecorded this period",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MedicationAdherenceCard(
    summary: MedicationAdherenceSummary
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth().testTag("insights_medication_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Medication, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Medication Schedule Adherence",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (summary.adherenceRate != null) {
                    val pct = (summary.adherenceRate * 100).toInt()
                    Text(
                        text = "$pct% Taken",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (summary.totalScheduledDoses == 0) {
                Text(
                    text = "No active medication doses scheduled in this period.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Taken on time (±60m):",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${summary.dosesTakenOnTime} doses",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (summary.dosesTakenDelayed > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Taken delayed (>60m):",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${summary.dosesTakenDelayed} doses",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }

                        if (summary.dosesSkipped > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Recorded skips:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${summary.dosesSkipped} doses",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        if (summary.dosesUnrecorded > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Unrecorded:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${summary.dosesUnrecorded} doses",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoodEnergyTrendsCard(
    summary: WellnessMetricSummary,
    sleepSummary: SleepMetricSummary
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth().testTag("insights_mood_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.SelfImprovement, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Mood & Wellness Patterns",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                TrendIconBadge(summary.moodTrend)
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (summary.recordedDaysCount == 0) {
                Text(
                    text = "No check-ins recorded in this period. Log morning or evening check-ins to view observations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Mood", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = summary.averageMood?.let { String.format(Locale.US, "%.1f", it) } ?: "--",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Energy", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = summary.averageEnergy?.let { String.format(Locale.US, "%.1f", it) } ?: "--",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Stress", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = summary.averageStress?.let { String.format(Locale.US, "%.1f", it) } ?: "--",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Observational non-diagnostic note
                val moodNote = when (summary.moodTrend) {
                    MetricTrend.TRENDING_HIGHER -> "Mood check-ins are trending higher compared to earlier in the period."
                    MetricTrend.TRENDING_LOWER -> "Mood check-ins are trending lower compared to earlier in the period."
                    MetricTrend.STEADY -> "Mood check-ins have remained steady over the period."
                    MetricTrend.INSUFFICIENT_DATA -> "Check in regularly to observe patterns over time."
                }
                Text(
                    text = moodNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (sleepSummary.averageDurationMinutes != null && summary.averageMood != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val avgSleepHours = sleepSummary.averageDurationMinutes / 60
                    Text(
                        text = "Across recorded days, average sleep of ${avgSleepHours}h corresponds with an average mood rating of ${String.format(Locale.US, "%.1f/5", summary.averageMood)}.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun TrendIconBadge(trend: MetricTrend) {
    when (trend) {
        MetricTrend.TRENDING_HIGHER -> {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = "Trending higher", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text("Rising", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        MetricTrend.TRENDING_LOWER -> {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = "Trending lower", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                Text("Lower", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            }
        }
        MetricTrend.STEADY -> {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.AutoMirrored.Filled.TrendingFlat, contentDescription = "Steady", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Text("Steady", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        MetricTrend.INSUFFICIENT_DATA -> Unit
    }
}

@Composable
fun FocusHabitsCard(
    summary: FocusMetricSummary
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth().testTag("insights_focus_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Focus & Learning Habits",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val h = summary.totalDurationMinutes / 60
                val m = summary.totalDurationMinutes % 60
                Text(
                    text = "${h}h ${m}m",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (summary.totalSessionsCount == 0) {
                Text(
                    text = "No focus sessions logged in this period.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "${summary.completedSessionsCount} of ${summary.totalSessionsCount} sessions completed across ${summary.activeDaysCount} active study days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (summary.categoryShares.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        summary.categoryShares.take(4).forEach { share ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = share.category,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${share.durationMinutes / 60}h ${share.durationMinutes % 60}m (${String.format(Locale.US, "%.0f%%", share.percentage)})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PersonalSummaryExportCard(
    startDate: String,
    endDate: String,
    sleepSummary: SleepMetricSummary,
    srmSummary: SrmMetricSummary,
    medicationSummary: MedicationAdherenceSummary,
    wellnessSummary: WellnessMetricSummary,
    focusSummary: FocusMetricSummary,
    healthCheckins: List<com.example.data.entity.HealthCheckinEntity>,
    routineEntries: List<com.example.data.entity.RoutineEntryEntity>,
    brainDumpNotes: List<com.example.data.entity.BrainDumpNoteEntity>,
    onCopyToClipboard: (String) -> Unit
) {
    var includePrivateNotes by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth().testTag("insights_export_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Personal Summary Export",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Generate a structured, privacy-preserving summary of your sleep, social rhythms, medication adherence, and focus habits for personal reflection or sharing with your doctor.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Privacy Toggle Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { includePrivateNotes = !includePrivateNotes }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = includePrivateNotes,
                    onCheckedChange = { includePrivateNotes = it },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("export_private_notes_checkbox")
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Include private text notes",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (includePrivateNotes) "Freeform symptoms and thoughts will be included" else "Private notes are redacted for privacy",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            FilledTonalButton(
                onClick = {
                    val summaryText = ReportExportManager.generateSummaryText(
                        startDate = startDate,
                        endDate = endDate,
                        sleepMetrics = sleepSummary,
                        srmMetrics = srmSummary,
                        medicationMetrics = medicationSummary,
                        wellnessMetrics = wellnessSummary,
                        focusMetrics = focusSummary,
                        includePrivateNotes = includePrivateNotes,
                        healthCheckins = healthCheckins,
                        routineEntries = routineEntries,
                        brainDumpNotes = brainDumpNotes
                    )
                    onCopyToClipboard(summaryText)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("export_copy_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy Summary to Clipboard")
            }
        }
    }
}
