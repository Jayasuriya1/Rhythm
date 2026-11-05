package com.example.ui.screens.study

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BreakPoint
import com.example.data.model.BreakScheduleMethod
import com.example.data.model.StudyProtocol

/**
 * Modern, highly intuitive Study Protocol Studio.
 *
 * Supports:
 * - Multi-Session Study Blocks (e.g. 3 Hours divided into 3x 50m sessions)
 * - Frequent Micro-Breaks in seconds or minutes (e.g. every 20m for 30s eye rest)
 * - Long Section Breaks between sessions (e.g. 15m walk & recharge)
 * - Custom break naming displayed on screen during timer
 * - Calming screen break visual effects (Breathing Guide Pulse, Ambient Ripple, Zen Glow, Minimal)
 * - Interactive visual timeline showing the entire study flow
 */
@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun StudyProtocolConfigDialog(
    initialProtocol: StudyProtocol,
    onSaveProtocol: (StudyProtocol) -> Unit,
    onDismiss: () -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0: Sprints Builder, 1: Curated Presets, 2: Screen Effects

    var sessionCount by remember { mutableIntStateOf(initialProtocol.sessionCount.coerceIn(1, 8)) }
    var sessionDurationMinutes by remember { mutableIntStateOf(initialProtocol.sessionDurationMinutes.coerceIn(5, 120)) }
    var enableMicroBreaks by remember { mutableStateOf(initialProtocol.microBreakIntervalMinutes > 0) }
    var microBreakIntervalMinutes by remember { mutableIntStateOf(if (initialProtocol.microBreakIntervalMinutes > 0) initialProtocol.microBreakIntervalMinutes else 20) }
    var microBreakDurationSeconds by remember { mutableIntStateOf(initialProtocol.microBreakDurationSeconds.coerceIn(10, 600)) }
    var microBreakLabel by remember { mutableStateOf(initialProtocol.microBreakLabel) }

    var enableLongBreaks by remember { mutableStateOf(initialProtocol.longBreakMinutes > 0) }
    var longBreakMinutes by remember { mutableIntStateOf(if (initialProtocol.longBreakMinutes > 0) initialProtocol.longBreakMinutes else 15) }
    var longBreakLabel by remember { mutableStateOf(initialProtocol.longBreakLabel) }

    var breakEffectStyle by remember { mutableStateOf(initialProtocol.breakEffectStyle) }
    var breaksCountTowardTotal by remember { mutableStateOf(initialProtocol.breaksCountTowardTotal) }
    var hasCooldownPeriod by remember { mutableStateOf(initialProtocol.hasCooldownPeriod) }
    var cooldownMinutes by remember { mutableIntStateOf(initialProtocol.cooldownMinutes) }

    val totalFocusMinutes = sessionCount * sessionDurationMinutes

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .widthIn(max = 580.dp)
                .fillMaxHeight(0.94f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.RocketLaunch,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Study Protocol Studio",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val totalHours = totalFocusMinutes / 60
                            val remMins = totalFocusMinutes % 60
                            val timeStr = if (totalHours > 0) "${totalHours}h ${remMins}m" else "${remMins}m"
                            Text(
                                text = "$sessionCount Sessions • $timeStr Total Focus",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_protocol_dialog")) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Interactive Visual Timeline Strip (Preview of whole block)
                ProtocolVisualTimelineBanner(
                    sessionCount = sessionCount,
                    sessionDurationMinutes = sessionDurationMinutes,
                    enableMicroBreaks = enableMicroBreaks,
                    microBreakIntervalMinutes = microBreakIntervalMinutes,
                    microBreakSeconds = microBreakDurationSeconds,
                    enableLongBreaks = enableLongBreaks,
                    longBreakMinutes = longBreakMinutes
                )

                // Tabs
                PrimaryTabRow(
                    selectedTabIndex = activeTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Custom Builder", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Curated Presets", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Spa, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = { Text("Screen Effects", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Air, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                // Body Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    when (activeTab) {
                        0 -> CustomBuilderTab(
                            sessionCount = sessionCount,
                            onSessionCountChange = { sessionCount = it },
                            sessionDurationMinutes = sessionDurationMinutes,
                            onSessionDurationChange = { sessionDurationMinutes = it },
                            enableMicroBreaks = enableMicroBreaks,
                            onToggleMicroBreaks = { enableMicroBreaks = it },
                            microBreakIntervalMinutes = microBreakIntervalMinutes,
                            onMicroIntervalChange = { microBreakIntervalMinutes = it },
                            microBreakDurationSeconds = microBreakDurationSeconds,
                            onMicroDurationChange = { microBreakDurationSeconds = it },
                            microBreakLabel = microBreakLabel,
                            onMicroLabelChange = { microBreakLabel = it },
                            enableLongBreaks = enableLongBreaks,
                            onToggleLongBreaks = { enableLongBreaks = it },
                            longBreakMinutes = longBreakMinutes,
                            onLongBreakMinutesChange = { longBreakMinutes = it },
                            longBreakLabel = longBreakLabel,
                            onLongBreakLabelChange = { longBreakLabel = it },
                            breaksCountTowardTotal = breaksCountTowardTotal,
                            onToggleBreaksCountTowardTotal = { breaksCountTowardTotal = it },
                            hasCooldownPeriod = hasCooldownPeriod,
                            onToggleCooldown = { hasCooldownPeriod = it },
                            cooldownMinutes = cooldownMinutes,
                            onCooldownMinutesChange = { cooldownMinutes = it }
                        )

                        1 -> CuratedPresetsTab(
                            onSelectPreset = { presetSessions, presetDur, presetMicroInt, presetMicroSec, presetMicroName, presetLongMins, presetLongName, presetEffect ->
                                sessionCount = presetSessions
                                sessionDurationMinutes = presetDur
                                enableMicroBreaks = presetMicroInt > 0
                                microBreakIntervalMinutes = if (presetMicroInt > 0) presetMicroInt else 20
                                microBreakDurationSeconds = presetMicroSec
                                microBreakLabel = presetMicroName
                                enableLongBreaks = presetLongMins > 0
                                longBreakMinutes = if (presetLongMins > 0) presetLongMins else 15
                                longBreakLabel = presetLongName
                                breakEffectStyle = presetEffect
                                activeTab = 0 // Switch to builder so user can inspect or customize
                            }
                        )

                        2 -> ScreenEffectsTab(
                            selectedEffect = breakEffectStyle,
                            onSelectEffect = { breakEffectStyle = it }
                        )
                    }
                }

                // Bottom Action Bar
                Surface(
                    tonalElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                val updatedProtocol = StudyProtocol(
                                    version = 2,
                                    totalFocusMinutes = sessionCount * sessionDurationMinutes,
                                    scheduleMethod = BreakScheduleMethod.MULTI_SESSION,
                                    sessionCount = sessionCount,
                                    sessionDurationMinutes = sessionDurationMinutes,
                                    microBreakIntervalMinutes = if (enableMicroBreaks) microBreakIntervalMinutes else 0,
                                    microBreakDurationSeconds = microBreakDurationSeconds,
                                    microBreakLabel = microBreakLabel.trim().ifBlank { "Micro-Break" },
                                    longBreakMinutes = if (enableLongBreaks) longBreakMinutes else 0,
                                    longBreakLabel = longBreakLabel.trim().ifBlank { "Section Recharge Break" },
                                    breakEffectStyle = breakEffectStyle,
                                    breaksCountTowardTotal = breaksCountTowardTotal,
                                    hasCooldownPeriod = hasCooldownPeriod,
                                    cooldownMinutes = cooldownMinutes
                                )
                                onSaveProtocol(updatedProtocol)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("apply_study_protocol_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Apply Protocol", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Visual graphic strip that displays sessions and breaks like a journey map.
 */
@Composable
private fun ProtocolVisualTimelineBanner(
    sessionCount: Int,
    sessionDurationMinutes: Int,
    enableMicroBreaks: Boolean,
    microBreakIntervalMinutes: Int,
    microBreakSeconds: Int,
    enableLongBreaks: Boolean,
    longBreakMinutes: Int
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BLOCK TIMELINE FLOW",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val totalFocus = sessionCount * sessionDurationMinutes
                val microCountPerSession = if (enableMicroBreaks && microBreakIntervalMinutes > 0 && microBreakIntervalMinutes < sessionDurationMinutes) {
                    (sessionDurationMinutes - 1) / microBreakIntervalMinutes
                } else 0
                val totalMicroSec = microCountPerSession * sessionCount * microBreakSeconds
                val totalLongMins = if (enableLongBreaks && sessionCount > 1) (sessionCount - 1) * longBreakMinutes else 0
                val totalBreakMinutes = (totalMicroSec / 60) + totalLongMins
                Text(
                    text = "Focus: ${totalFocus}m • Breaks: ${totalBreakMinutes}m",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable Timeline Pills
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                for (s in 1..sessionCount) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(
                                    text = "Session $s",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                val microCount = if (enableMicroBreaks && microBreakIntervalMinutes > 0 && microBreakIntervalMinutes < sessionDurationMinutes) {
                                    (sessionDurationMinutes - 1) / microBreakIntervalMinutes
                                } else 0
                                val microText = if (microCount > 0) {
                                    val secLabel = if (microBreakSeconds < 60) "${microBreakSeconds}s" else "${microBreakSeconds / 60}m"
                                    "${sessionDurationMinutes}m ($microCount × $secLabel)"
                                } else {
                                    "${sessionDurationMinutes}m"
                                }
                                Text(
                                    text = microText,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    if (s < sessionCount && enableLongBreaks && longBreakMinutes > 0) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Coffee,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${longBreakMinutes}m Rest",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
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
 * Tab 1: Custom Builder
 */
@Composable
private fun CustomBuilderTab(
    sessionCount: Int,
    onSessionCountChange: (Int) -> Unit,
    sessionDurationMinutes: Int,
    onSessionDurationChange: (Int) -> Unit,
    enableMicroBreaks: Boolean,
    onToggleMicroBreaks: (Boolean) -> Unit,
    microBreakIntervalMinutes: Int,
    onMicroIntervalChange: (Int) -> Unit,
    microBreakDurationSeconds: Int,
    onMicroDurationChange: (Int) -> Unit,
    microBreakLabel: String,
    onMicroLabelChange: (String) -> Unit,
    enableLongBreaks: Boolean,
    onToggleLongBreaks: (Boolean) -> Unit,
    longBreakMinutes: Int,
    onLongBreakMinutesChange: (Int) -> Unit,
    longBreakLabel: String,
    onLongBreakLabelChange: (String) -> Unit,
    breaksCountTowardTotal: Boolean,
    onToggleBreaksCountTowardTotal: (Boolean) -> Unit,
    hasCooldownPeriod: Boolean,
    onToggleCooldown: (Boolean) -> Unit,
    cooldownMinutes: Int,
    onCooldownMinutesChange: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Session Breakdown & Durations
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sessions in Block",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onSessionCountChange(maxOf(1, sessionCount - 1)) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease Sessions")
                            }
                            Text(
                                text = "$sessionCount",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(
                                onClick = { onSessionCountChange(minOf(8, sessionCount + 1)) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase Sessions")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Session Length: $sessionDurationMinutes min",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf(25, 40, 45, 50, 60, 90)) { mins ->
                            FilterChip(
                                selected = sessionDurationMinutes == mins,
                                onClick = { onSessionDurationChange(mins) },
                                label = { Text("${mins}m") }
                            )
                        }
                    }
                }
            }
        }

        // 2. Frequent Micro-Breaks (seconds or minutes)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Frequent Micro-Breaks",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Quick 20s-60s eye and posture breaks inside each session",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enableMicroBreaks,
                            onCheckedChange = onToggleMicroBreaks,
                            modifier = Modifier.testTag("toggle_micro_breaks")
                        )
                    }

                    AnimatedVisibility(visible = enableMicroBreaks) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Text(
                                text = "Micro-Break Frequency",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(15, 20, 25, 30).forEach { intMins ->
                                    FilterChip(
                                        selected = microBreakIntervalMinutes == intMins,
                                        onClick = { onMicroIntervalChange(intMins) },
                                        label = { Text("Every ${intMins}m") }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Micro-Break Length (Seconds or Minutes)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val secondsPresets = listOf(
                                    15 to "15s",
                                    20 to "20s (20-20-20)",
                                    30 to "30s",
                                    45 to "45s",
                                    60 to "1 min",
                                    120 to "2 min",
                                    180 to "3 min"
                                )
                                items(secondsPresets) { (secs, label) ->
                                    FilterChip(
                                        selected = microBreakDurationSeconds == secs,
                                        onClick = { onMicroDurationChange(secs) },
                                        label = { Text(label) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = microBreakLabel,
                                onValueChange = onMicroLabelChange,
                                label = { Text("Break Name (Shown on Timer)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val suggestions = listOf(
                                    "20-20-20 Eye Rest",
                                    "Deep Breathing",
                                    "Hydration & Stretch",
                                    "Posture Reset"
                                )
                                items(suggestions) { sugg ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        modifier = Modifier.clickable { onMicroLabelChange(sugg) }
                                    ) {
                                        Text(
                                            text = "+ $sugg",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Long Section Breaks between Sessions
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Long Section Breaks",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Rest periods between full study sessions (e.g. coffee, walk)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enableLongBreaks,
                            onCheckedChange = onToggleLongBreaks,
                            modifier = Modifier.testTag("toggle_long_breaks")
                        )
                    }

                    AnimatedVisibility(visible = enableLongBreaks) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Text(
                                text = "Long Break Duration",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(5, 10, 15, 20, 30).forEach { mins ->
                                    FilterChip(
                                        selected = longBreakMinutes == mins,
                                        onClick = { onLongBreakMinutesChange(mins) },
                                        label = { Text("${mins}m") }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = longBreakLabel,
                                onValueChange = onLongBreakLabelChange,
                                label = { Text("Long Break Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val suggestions = listOf(
                                    "Section Recharge Break",
                                    "Coffee & Walk",
                                    "Meal & Rest",
                                    "Movement & Refresh"
                                )
                                items(suggestions) { sugg ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                        modifier = Modifier.clickable { onLongBreakLabelChange(sugg) }
                                    ) {
                                        Text(
                                            text = "+ $sugg",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Session Options: Cooldown & Breaks Count
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Final Review / Cooldown Period", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("5-minute calm wind-down after completing the block", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = hasCooldownPeriod, onCheckedChange = onToggleCooldown)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Breaks Count Inside Total Block", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("If off, breaks pause focus countdown and add to session length", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = breaksCountTowardTotal, onCheckedChange = onToggleBreaksCountTowardTotal)
                    }
                }
            }
        }
    }
}

/**
 * Tab 2: Curated Presets
 */
@Composable
private fun CuratedPresetsTab(
    onSelectPreset: (
        sessionCount: Int,
        sessionDur: Int,
        microInt: Int,
        microSec: Int,
        microName: String,
        longMins: Int,
        longName: String,
        effectStyle: String
    ) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PresetCard(
                icon = Icons.Default.RocketLaunch,
                title = "3-Hour Deep Mastery Block",
                badge = "RECOMMENDED FOR EXTENDED STUDY",
                description = "3 sessions × 50m • 15m Long Section Breaks • 30s Eye-Rest Micro-Breaks every 20m",
                details = "Ideal for complex coding, exam preparation, or dissertation writing without mental fatigue.",
                onClick = {
                    onSelectPreset(
                        3, // 3 sessions
                        50, // 50m each = 150m focus (2.5h) + 30m break = 3h total block
                        20, // micro break every 20m
                        30, // 30 seconds
                        "20-20-20 Eye Rest & Breathe",
                        15, // 15m long break
                        "Section Recharge Break",
                        "BREATHING_GUIDE"
                    )
                }
            )
        }

        item {
            PresetCard(
                icon = Icons.Default.Speed,
                title = "Classic Pomodoro Block (2 Hours)",
                badge = "CLASSIC PRODUCTIVITY",
                description = "4 sessions × 25m • 15m Long Section Break • 5m Short Breaks between sprints",
                details = "High-energy interval structure that keeps focus sharp with rapid iteration.",
                onClick = {
                    onSelectPreset(
                        4,
                        25,
                        0, // no micro breaks inside 25m sprint
                        0,
                        "",
                        5, // 5m between sprints
                        "5-Minute Rest",
                        "AMBIENT_RIPPLE"
                    )
                }
            )
        }

        item {
            PresetCard(
                icon = Icons.Default.SelfImprovement,
                title = "90-Minute Ultradian Sprint",
                badge = "BIOLOGICAL RHYTHM",
                description = "1 session × 90m Deep Dive • 30s Posture & Eye checks every 20m • 20m Cooldown",
                details = "Aligned with human 90-minute ultradian cycles for peak cognitive performance.",
                onClick = {
                    onSelectPreset(
                        1,
                        90,
                        20,
                        30,
                        "Posture & Blink Check",
                        20,
                        "Ultradian Recovery Break",
                        "ZEN_GLOW"
                    )
                }
            )
        }

        item {
            PresetCard(
                icon = Icons.Default.Spa,
                title = "Calm 1-Hour Focus Sprint",
                badge = "GENTLE & BALANCED",
                description = "2 sessions × 30m • 5m Stretch Break • 20s Eye Rest every 15m",
                details = "Perfect for reading, documentation review, or daily habit consistency.",
                onClick = {
                    onSelectPreset(
                        2,
                        30,
                        15,
                        20,
                        "20-20-20 Eye Rest",
                        5,
                        "Stretch & Hydrate",
                        "BREATHING_GUIDE"
                    )
                }
            )
        }
    }
}

@Composable
private fun PresetCard(
    icon: ImageVector,
    title: String,
    badge: String,
    description: String,
    details: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = details,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select This Protocol")
            }
        }
    }
}

/**
 * Tab 3: Screen Effects during Breaks
 */
@Composable
private fun ScreenEffectsTab(
    selectedEffect: String,
    onSelectEffect: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Screen Effects During Break",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Choose the relaxing visual animation that displays on screen when a micro-break or section break triggers.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val effects = listOf(
            Triple(
                "BREATHING_GUIDE",
                "🌬️ Breathing Pulse Guide (Recommended)",
                "Expanding and contracting calming circle with gentle Inhale (4s) • Hold (4s) • Exhale (4s) text guidance."
            ),
            Triple(
                "AMBIENT_RIPPLE",
                "🌊 Ambient Wave Ripples",
                "Smooth concentric relaxing waves gently pulsating outwards to soothe eye fatigue."
            ),
            Triple(
                "ZEN_GLOW",
                "✨ Zen Starlight Glow",
                "Soft celestial glowing ambient backdrop with calming aura shifts."
            ),
            Triple(
                "MINIMAL",
                "🎯 High-Contrast Minimal",
                "Clean, distraction-free countdown digits with minimalist progress indicator."
            )
        )

        items(effects) { (id, name, desc) ->
            val isSelected = selectedEffect == id
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectEffect(id) }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = desc,
                            fontSize = 11.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
