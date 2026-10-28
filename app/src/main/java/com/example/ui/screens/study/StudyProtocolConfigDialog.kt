package com.example.ui.screens.study

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BreakPoint
import com.example.data.model.BreakScheduleMethod
import com.example.data.model.StudyProtocol

/**
 * Dialog for configuring arbitrary study sessions, interval micro-breaks,
 * custom timeline break points, cooldowns, and audio cues.
 */
@Composable
fun StudyProtocolConfigDialog(
    initialProtocol: StudyProtocol,
    onSaveProtocol: (StudyProtocol) -> Unit,
    onDismiss: () -> Unit
) {
    var totalFocusMinutes by remember { mutableIntStateOf(initialProtocol.totalFocusMinutes) }
    var scheduleMethod by remember { mutableStateOf(initialProtocol.scheduleMethod) }
    var intervalMinutes by remember { mutableIntStateOf(initialProtocol.intervalMinutes) }
    var defaultBreakMinutes by remember { mutableIntStateOf(initialProtocol.defaultBreakMinutes) }
    var longBreakMinutes by remember { mutableIntStateOf(initialProtocol.longBreakMinutes) }
    var longBreakIntervalCount by remember { mutableIntStateOf(initialProtocol.longBreakIntervalCount) }
    var hasCooldownPeriod by remember { mutableStateOf(initialProtocol.hasCooldownPeriod) }
    var cooldownMinutes by remember { mutableIntStateOf(initialProtocol.cooldownMinutes) }
    var breaksCountTowardTotal by remember { mutableStateOf(initialProtocol.breaksCountTowardTotal) }

    val customBreakPoints = remember {
        mutableStateListOf<BreakPoint>().apply {
            addAll(initialProtocol.customBreakPoints.ifEmpty {
                listOf(
                    BreakPoint(triggerElapsedMinute = 15, breakDurationMinutes = 3, label = "Eye Rest"),
                    BreakPoint(triggerElapsedMinute = 30, breakDurationMinutes = 5, label = "Water & Stretch"),
                    BreakPoint(triggerElapsedMinute = 45, breakDurationMinutes = 3, label = "Breathe")
                )
            })
        }
    }

    var newPointMinute by remember { mutableStateOf("") }
    var newPointDuration by remember { mutableStateOf("5") }
    var newPointLabel by remember { mutableStateOf("Micro-Break") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Coffee, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Study Protocol Studio", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Total Focus Duration
                item {
                    Column {
                        Text(
                            text = "Total Focus Duration: $totalFocusMinutes min",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(25, 45, 50, 60, 90, 120).forEach { mins ->
                                FilterChip(
                                    selected = totalFocusMinutes == mins,
                                    onClick = { totalFocusMinutes = mins },
                                    label = { Text("${mins}m") },
                                    modifier = Modifier.testTag("protocol_duration_${mins}m")
                                )
                            }
                        }
                    }
                }

                // Scheduling Method
                item {
                    Column {
                        Text(
                            text = "Break Scheduling Strategy",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = scheduleMethod == BreakScheduleMethod.INTERVAL,
                                onClick = { scheduleMethod = BreakScheduleMethod.INTERVAL },
                                label = { Text("Repeating Interval") },
                                modifier = Modifier.testTag("method_interval_chip")
                            )
                            FilterChip(
                                selected = scheduleMethod == BreakScheduleMethod.CUSTOM_TIMELINE,
                                onClick = { scheduleMethod = BreakScheduleMethod.CUSTOM_TIMELINE },
                                label = { Text("Custom Timeline") },
                                modifier = Modifier.testTag("method_custom_chip")
                            )
                        }
                    }
                }

                if (scheduleMethod == BreakScheduleMethod.INTERVAL) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Interval Parameters", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Break every:", style = MaterialTheme.typography.bodyMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(15, 20, 25, 30).forEach { mins ->
                                            FilterChip(
                                                selected = intervalMinutes == mins,
                                                onClick = { intervalMinutes = mins },
                                                label = { Text("${mins}m") }
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Micro-break length:", style = MaterialTheme.typography.bodyMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(3, 5, 7, 10).forEach { mins ->
                                            FilterChip(
                                                selected = defaultBreakMinutes == mins,
                                                onClick = { defaultBreakMinutes = mins },
                                                label = { Text("${mins}m") }
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Long break length:", style = MaterialTheme.typography.bodyMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(10, 15, 20).forEach { mins ->
                                            FilterChip(
                                                selected = longBreakMinutes == mins,
                                                onClick = { longBreakMinutes = mins },
                                                label = { Text("${mins}m") }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Custom Timeline Break Points
                    item {
                        Column {
                            Text(
                                text = "Custom Break Points (${customBreakPoints.size})",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Enter elapsed-time points into the session (e.g. at 12m, 27m, 44m).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    itemsIndexed(customBreakPoints) { index, bp ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${bp.label} at minute ${bp.triggerElapsedMinute}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "Duration: ${bp.breakDurationMinutes} min",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (index > 0) {
                                        IconButton(onClick = {
                                            val item = customBreakPoints.removeAt(index)
                                            customBreakPoints.add(index - 1, item)
                                        }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    if (index < customBreakPoints.size - 1) {
                                        IconButton(onClick = {
                                            val item = customBreakPoints.removeAt(index)
                                            customBreakPoints.add(index + 1, item)
                                        }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    IconButton(onClick = {
                                        customBreakPoints.removeAt(index)
                                    }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Break Point", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Add Break Point Form
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Add Break Point", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = newPointMinute,
                                        onValueChange = { newPointMinute = it.filter { c -> c.isDigit() } },
                                        label = { Text("At minute") },
                                        placeholder = { Text("e.g. 25") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("input_break_minute")
                                    )
                                    OutlinedTextField(
                                        value = newPointDuration,
                                        onValueChange = { newPointDuration = it.filter { c -> c.isDigit() } },
                                        label = { Text("Length (m)") },
                                        placeholder = { Text("5") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("input_break_duration")
                                    )
                                }

                                OutlinedTextField(
                                    value = newPointLabel,
                                    onValueChange = { newPointLabel = it },
                                    label = { Text("Label") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("input_break_label")
                                )

                                Button(
                                    onClick = {
                                        val min = newPointMinute.toIntOrNull()
                                        val dur = newPointDuration.toIntOrNull()
                                        if (min == null || min <= 0 || min >= totalFocusMinutes) {
                                            validationError = "Trigger minute must be between 1 and ${totalFocusMinutes - 1}."
                                            return@Button
                                        }
                                        if (dur == null || dur <= 0) {
                                            validationError = "Break duration must be at least 1 minute."
                                            return@Button
                                        }
                                        if (customBreakPoints.any { it.triggerElapsedMinute == min }) {
                                            validationError = "A break point at minute $min already exists."
                                            return@Button
                                        }

                                        customBreakPoints.add(
                                            BreakPoint(
                                                triggerElapsedMinute = min,
                                                breakDurationMinutes = dur,
                                                label = newPointLabel.trim().ifBlank { "Micro-Break" },
                                                isLongBreak = dur >= 12
                                            )
                                        )
                                        newPointMinute = ""
                                        validationError = null
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("btn_add_break_point")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Break Point")
                                }
                            }
                        }
                    }
                }

                // Cooldown & Total Calculation Options
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Final Session Cooldown", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text("Add a 5-minute review/wind-down period at completion", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = hasCooldownPeriod,
                                onCheckedChange = { hasCooldownPeriod = it },
                                modifier = Modifier.testTag("cooldown_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Breaks Count Inside Total Block", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text(
                                    text = if (breaksCountTowardTotal) "Session ends exactly at $totalFocusMinutes mins total" else "Breaks pause focus countdown (adds break time to session)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = breaksCountTowardTotal,
                                onCheckedChange = { breaksCountTowardTotal = it },
                                modifier = Modifier.testTag("breaks_count_total_switch")
                            )
                        }
                    }
                }

                if (validationError != null) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Text(validationError ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val protocol = StudyProtocol(
                        totalFocusMinutes = totalFocusMinutes,
                        scheduleMethod = scheduleMethod,
                        intervalMinutes = intervalMinutes,
                        defaultBreakMinutes = defaultBreakMinutes,
                        longBreakMinutes = longBreakMinutes,
                        longBreakIntervalCount = longBreakIntervalCount,
                        customBreakPoints = customBreakPoints.sortedBy { it.triggerElapsedMinute },
                        hasCooldownPeriod = hasCooldownPeriod,
                        cooldownMinutes = cooldownMinutes,
                        breaksCountTowardTotal = breaksCountTowardTotal,
                        startSoundEnabled = initialProtocol.startSoundEnabled,
                        breakStartSoundEnabled = initialProtocol.breakStartSoundEnabled,
                        breakEndSoundEnabled = initialProtocol.breakEndSoundEnabled,
                        sessionEndSoundEnabled = initialProtocol.sessionEndSoundEnabled
                    )

                    val error = protocol.validate()
                    if (error != null) {
                        validationError = error
                    } else {
                        onSaveProtocol(protocol)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_save_protocol_button")
            ) {
                Text("Apply Protocol")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
