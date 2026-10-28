package com.example.ui.screens.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.RoutineAnchorEntity
import com.example.data.entity.RoutineEntryEntity
import com.example.data.entity.StudyTaskEntity
import java.util.Calendar

/**
 * Modern Material 3 Circular Arc Agenda Card.
 * Dynamically computes the next scheduled routine anchor or priority focus event,
 * rendering a circular arc progress representation with countdown and direct action trigger.
 */
@Composable
fun NextEventCircularArcCard(
    anchors: List<RoutineAnchorEntity>,
    entries: List<RoutineEntryEntity>,
    dailyPriorityTask: StudyTaskEntity?,
    onDoneNow: (RoutineAnchorEntity) -> Unit,
    onStartFocus: (StudyTaskEntity) -> Unit,
    onViewSchedule: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cal = Calendar.getInstance()
    val currentMinuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

    val completedAnchorIds = entries.filter { it.status == "COMPLETED" }.map { it.anchorId }.toSet()
    val upcomingAnchors = anchors
        .filter { !completedAnchorIds.contains(it.id) }
        .sortedBy { it.defaultTargetMinutes }

    val nextAnchor = upcomingAnchors.firstOrNull { it.defaultTargetMinutes >= currentMinuteOfDay }
        ?: upcomingAnchors.firstOrNull()

    // Determine circular arc sweep (day progress: 0 to 1)
    val dayProgress = (currentMinuteOfDay.toFloat() / (24f * 60f)).coerceIn(0f, 1f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val accentColor = MaterialTheme.colorScheme.tertiary

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("next_event_agenda_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                        text = "Next on Your Agenda",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                ) {
                    val timeStr = String.format(java.util.Locale.getDefault(), "%02d:%02d", currentMinuteOfDay / 60, currentMinuteOfDay % 60)
                    Text(
                        text = "Now: $timeStr",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Circular Arc with Center Countdown
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                Canvas(modifier = Modifier.size(150.dp)) {
                    val strokeWidth = 12.dp.toPx()
                    val arcSize = size.width - strokeWidth
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                    // Track Background Arc (270 degrees sweep)
                    drawArc(
                        color = trackColor,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Active Day Progress Arc
                    val sweep = 270f * dayProgress
                    drawArc(
                        color = primaryColor,
                        startAngle = 135f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (nextAnchor != null) {
                        val diffMinutes: Int = nextAnchor.defaultTargetMinutes - currentMinuteOfDay
                        val diffHours: Int = diffMinutes / 60
                        val diffRemMins: Int = kotlin.math.abs(diffMinutes % 60)

                        val countdownText = when {
                            diffMinutes > 60 -> "in ${diffHours}h ${diffRemMins}m"
                            diffMinutes in 1..60 -> "in ${diffMinutes}m"
                            diffMinutes == 0 -> "Due Now"
                            else -> "${kotlin.math.abs(diffMinutes)}m ago"
                        }

                        Text(
                            text = countdownText,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (diffMinutes <= 0) accentColor else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = String.format(java.util.Locale.getDefault(), "%02d:%02d", nextAnchor.defaultTargetMinutes / 60, nextAnchor.defaultTargetMinutes % 60),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (dailyPriorityTask != null) {
                        Text(
                            text = "Focus Target",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${dailyPriorityTask.estimatedMinutes}m planned",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "All Done",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Relax & rest",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Event Details & Action Buttons
            if (nextAnchor != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = nextAnchor.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (nextAnchor.description.isNotBlank()) {
                            Text(
                                text = nextAnchor.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onDoneNow(nextAnchor) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("agenda_done_now_button")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Complete")
                            }

                            OutlinedButton(
                                onClick = onViewSchedule,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("agenda_view_routine_button")
                            ) {
                                Text("Full Routine")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            } else if (dailyPriorityTask != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = dailyPriorityTask.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Daily Study Priority",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { onStartFocus(dailyPriorityTask) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("agenda_start_focus_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Focus Session")
                        }
                    }
                }
            } else {
                Text(
                    text = "No pending routine anchors for today. You are completely on track.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
