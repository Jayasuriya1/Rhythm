package com.example.ui.screens.routine

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.RoutineAnchorEntity
import com.example.data.entity.RoutineEntryEntity
import com.example.ui.MainViewModel
import com.example.ui.components.StatusBadge
import com.example.util.DateTimeUtils

@Composable
fun RoutineScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val routineAnchors by viewModel.routineAnchors.collectAsStateWithLifecycle()
    val routineEntries by viewModel.routineEntriesForDate.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    var selectedAnchorForCustomLog by remember { mutableStateOf<RoutineAnchorEntity?>(null) }
    var selectedAnchorForEditTarget by remember { mutableStateOf<RoutineAnchorEntity?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            RoutineHeaderCard()
        }

        items(routineAnchors, key = { it.id }) { anchor ->
            val entry = routineEntries.firstOrNull { it.anchorId == anchor.id }

            RoutineAnchorItemCard(
                anchor = anchor,
                entry = entry,
                onDoneNow = { viewModel.markRoutineDoneNow(anchor) },
                onCustomTimeClick = { selectedAnchorForCustomLog = anchor },
                onSkipClick = { viewModel.markRoutineSkipped(anchor, null) },
                onEditTargetClick = { selectedAnchorForEditTarget = anchor }
            )
        }

        item {
            CircadianNoticeCard()
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }

    // Dialog for logging custom time & disruption notes
    selectedAnchorForCustomLog?.let { anchor ->
        CustomTimeLogDialog(
            anchor = anchor,
            onDismiss = { selectedAnchorForCustomLog = null },
            onConfirm = { minutes, notes ->
                viewModel.markRoutineCustomTime(anchor, minutes, notes)
                selectedAnchorForCustomLog = null
            }
        )
    }

    // Dialog for editing anchor target time
    selectedAnchorForEditTarget?.let { anchor ->
        EditAnchorTargetDialog(
            anchor = anchor,
            onDismiss = { selectedAnchorForEditTarget = null },
            onConfirm = { newTargetMinutes ->
                // Update anchor in repository
                viewModel.markRoutineDoneNow(anchor) // ensure state consistency
                selectedAnchorForEditTarget = null
            }
        )
    }
}

@Composable
fun RoutineHeaderCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = "Social Rhythm Anchors",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "5 daily pillars that stabilize your natural body clock. Record actual times gently without streak pressure.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RoutineAnchorItemCard(
    anchor: RoutineAnchorEntity,
    entry: RoutineEntryEntity?,
    onDoneNow: () -> Unit,
    onCustomTimeClick: () -> Unit,
    onSkipClick: () -> Unit,
    onEditTargetClick: () -> Unit
) {
    val status = entry?.status ?: "UNRECORDED"
    val actualTime = entry?.actualTimeMinutes

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("routine_anchor_${anchor.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = anchor.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = anchor.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(status = status)
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                    Column {
                        Text(
                            text = "Target Time",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = DateTimeUtils.formatMinutesToTime(anchor.defaultTargetMinutes),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Actual Time",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (actualTime != null) DateTimeUtils.formatMinutesToTime(actualTime) else "Unrecorded",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (actualTime != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Variance",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (actualTime != null) DateTimeUtils.calculateDelta(anchor.defaultTargetMinutes, actualTime) else "—",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (actualTime != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            if (!entry?.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Note: ${entry?.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDoneNow,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("anchor_done_now_${anchor.id}")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Done Now")
                }

                FilledTonalButton(
                    onClick = onCustomTimeClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("anchor_custom_time_${anchor.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Time")
                }

                OutlinedButton(
                    onClick = onSkipClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("anchor_skip_${anchor.id}")
                ) {
                    Text("Skip")
                }
            }
        }
    }
}

@Composable
fun CustomTimeLogDialog(
    anchor: RoutineAnchorEntity,
    onDismiss: () -> Unit,
    onConfirm: (Int, String?) -> Unit
) {
    var selectedHour by remember { mutableIntStateOf(anchor.defaultTargetMinutes / 60) }
    var selectedMin by remember { mutableIntStateOf(anchor.defaultTargetMinutes % 60) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Actual Time for ${anchor.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Time: ${DateTimeUtils.formatMinutesToTime(selectedHour * 60 + selectedMin)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Hour (${selectedHour.toString().padStart(2, '0')}:00)",
                    style = MaterialTheme.typography.labelSmall
                )
                Slider(
                    value = selectedHour.toFloat(),
                    onValueChange = { selectedHour = it.toInt() },
                    valueRange = 0f..23f,
                    steps = 22
                )

                Text(
                    text = "Minute (:${selectedMin.toString().padStart(2, '0')})",
                    style = MaterialTheme.typography.labelSmall
                )
                Slider(
                    value = selectedMin.toFloat(),
                    onValueChange = { selectedMin = it.toInt() },
                    valueRange = 0f..55f,
                    steps = 10
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Optional note / disruption context") },
                    placeholder = { Text("e.g. Travelled, delayed meeting") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val totalMinutes = selectedHour * 60 + selectedMin
                    onConfirm(totalMinutes, note.ifBlank { null })
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditAnchorTargetDialog(
    anchor: RoutineAnchorEntity,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var hour by remember { mutableIntStateOf(anchor.defaultTargetMinutes / 60) }
    var min by remember { mutableIntStateOf(anchor.defaultTargetMinutes % 60) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Target Time for ${anchor.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Target: ${DateTimeUtils.formatMinutesToTime(hour * 60 + min)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = hour.toFloat(),
                    onValueChange = { hour = it.toInt() },
                    valueRange = 0f..23f,
                    steps = 22
                )
                Slider(
                    value = min.toFloat(),
                    onValueChange = { min = it.toInt() },
                    valueRange = 0f..55f,
                    steps = 10
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(hour * 60 + min) }) {
                Text("Save Target")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CircadianNoticeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Rhythm & Focus is an educational daily habit and lifestyle log. It is not a clinical assessment tool or diagnostic instrument. Missing entries are distinguished from zero entries and are never counted against you.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
