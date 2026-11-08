package com.example.ui.screens.health

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.HealthConnectDailySummaryEntity
import com.example.data.entity.HealthConnectSleepStageEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.HealthMetricSleep
import com.example.ui.theme.SleepStageAwake
import com.example.ui.theme.SleepStageRem
import com.example.ui.theme.SleepStageLight
import com.example.ui.theme.SleepStageDeep
import com.example.ui.theme.SleepStageUnknown
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepAnalyticsScreen(
    viewModel: MainViewModel,
    isExpandedScreen: Boolean,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val dailySummary by viewModel.getHealthConnectSummaryForDate(selectedDate).collectAsStateWithLifecycle(initialValue = null)
    val sleepStages by viewModel.getHealthConnectSleepStagesForDate(selectedDate).collectAsStateWithLifecycle(initialValue = emptyList())
    val recentSummaries by viewModel.getRecentHealthConnectSummaries(7).collectAsStateWithLifecycle(initialValue = emptyList())

    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.US) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Sleep & Stage Analytics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Verified sleep duration & hypnogram stages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("sleep_analytics_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Duration & Times Card
            item {
                SleepDurationSummaryCard(summary = dailySummary, timeFormat = timeFormat)
            }

            // Hypnogram / Stage Timeline
            item {
                HypnogramCard(
                    summary = dailySummary,
                    stages = sleepStages,
                    timeFormat = timeFormat
                )
            }

            // 7-Day Sleep Duration Trend
            item {
                WeeklySleepTrendCard(summaries = recentSummaries)
            }

            // Detailed stages breakdown or explanation
            item {
                SleepStagesBreakdownCard(stages = sleepStages)
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SleepDurationSummaryCard(
    summary: HealthConnectDailySummaryEntity?,
    timeFormat: SimpleDateFormat,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = HealthMetricSleep.copy(alpha = 0.2f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.Bedtime, contentDescription = null, tint = HealthMetricSleep, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text("Total Sleep", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }

                if (summary?.primarySourceApp != null) {
                    val appName = summary.primarySourceApp.substringAfterLast(".").replaceFirstChar { it.uppercase() }
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.background) {
                        Text(text = appName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }

            val durationMinutes = summary?.sleepDurationMinutes ?: 0
            val hours = durationMinutes / 60
            val mins = durationMinutes % 60

            Text(
                text = if (durationMinutes > 0) "${hours}h ${mins}m" else "No sleep record",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Bedtime and Wake time timestamps
            if (summary?.sleepStartTimeEpoch != null && summary.sleepEndTimeEpoch != null) {
                val bedStr = timeFormat.format(Date(summary.sleepStartTimeEpoch))
                val wakeStr = timeFormat.format(Date(summary.sleepEndTimeEpoch))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column {
                        Text("Bedtime", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(bedStr, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Column {
                        Text("Wake Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(wakeStr, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

/**
 * Honest Hypnogram: Uses actual recorded stage intervals from Health Connect.
 * If stages are missing, clearly explains that the wearable provided total duration only.
 */
@Composable
fun HypnogramCard(
    summary: HealthConnectDailySummaryEntity?,
    stages: List<HealthConnectSleepStageEntity>,
    timeFormat: SimpleDateFormat,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "Sleep Stage Timeline (Hypnogram)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (stages.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text(
                            text = if (summary != null && summary.sleepDurationMinutes > 0) {
                                "Your wearable reported a single sleep duration session, but did not export detailed sleep stage intervals (Deep, Light, REM, Awake) into Health Connect. Stage tracking requires a compatible wearable with sleep stage writing permissions enabled."
                            } else {
                                "No sleep session found for this date in Health Connect."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Render real timeline Canvas
                val minStart = stages.minOf { it.stageStartTimeEpoch }
                val maxEnd = stages.maxOf { it.stageEndTimeEpoch }
                val totalSpan = (maxEnd - minStart).coerceAtLeast(1L)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                ) {
                    val w = size.width
                    val h = size.height

                    // Grid lines for stages
                    val yAwake = 0f
                    val yRem = h * 0.28f
                    val yLight = h * 0.56f
                    val yDeep = h * 0.84f

                    // Draw stage blocks
                    for (stage in stages) {
                        val leftFrac = (stage.stageStartTimeEpoch - minStart).toFloat() / totalSpan
                        val rightFrac = (stage.stageEndTimeEpoch - minStart).toFloat() / totalSpan
                        val blockLeft = leftFrac * w
                        val blockWidth = (rightFrac - leftFrac) * w

                        val (stageY, stageColor) = when (stage.stageType) {
                            "AWAKE" -> Pair(yAwake, SleepStageAwake)
                            "REM" -> Pair(yRem, SleepStageRem)
                            "LIGHT" -> Pair(yLight, SleepStageLight)
                            "DEEP" -> Pair(yDeep, SleepStageDeep)
                            else -> Pair(yLight, SleepStageUnknown)
                        }

                        drawRoundRect(
                            color = stageColor,
                            topLeft = Offset(blockLeft, stageY),
                            size = Size(blockWidth.coerceAtLeast(2f), 18.dp.toPx()),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }

                // Stage Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StageLegendPill("Deep", SleepStageDeep)
                    StageLegendPill("Light", SleepStageLight)
                    StageLegendPill("REM", SleepStageRem)
                    StageLegendPill("Awake", SleepStageAwake)
                }
            }
        }
    }
}

@Composable
fun StageLegendPill(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun WeeklySleepTrendCard(
    summaries: List<HealthConnectDailySummaryEntity>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "Last 7 Days Sleep Duration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (summaries.isEmpty()) {
                Text(
                    text = "No historical sleep records available in Health Connect for the last 7 days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    val barWidth = 24.dp.toPx()
                    val count = summaries.size
                    val spacing = (size.width - (barWidth * count)) / (count + 1).coerceAtLeast(1)
                    val maxMinutes = summaries.maxOfOrNull { it.sleepDurationMinutes }?.coerceAtLeast(480) ?: 480

                    summaries.forEachIndexed { index, s ->
                        val barHeight = (s.sleepDurationMinutes.toFloat() / maxMinutes.toFloat()) * size.height * 0.85f
                        val x = spacing + index * (barWidth + spacing)
                        val y = size.height - barHeight

                        drawRoundRect(
                            color = HealthMetricSleep,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }

                // Days labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    for (s in summaries) {
                        val dayLabel = s.date.takeLast(2)
                        Text(dayLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun SleepStagesBreakdownCard(
    stages: List<HealthConnectSleepStageEntity>,
    modifier: Modifier = Modifier
) {
    if (stages.isEmpty()) return

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Sleep Stages Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)

            val grouped = stages.groupBy { it.stageType }
            for ((type, list) in grouped) {
                val totalMins = list.sumOf { it.durationMinutes }
                val hrs = totalMins / 60
                val mins = totalMins % 60
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(type.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
