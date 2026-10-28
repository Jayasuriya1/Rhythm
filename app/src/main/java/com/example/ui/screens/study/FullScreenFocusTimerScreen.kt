package com.example.ui.screens.study

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.TimerMode
import com.example.ui.TimerState

/**
 * Phase 4 — Distraction-Free Full-Screen Study Timer Screen.
 * Requirements:
 * - Highly readable, large timer
 * - Current study task & category
 * - Clear exit button (with BackHandler)
 * - Start, pause, resume, reset, finish controls
 * - Optional session notes input
 * - Minimal visual distractions
 * - Restores active state seamlessly from timestamp-anchored timer
 */
@Composable
fun FullScreenFocusTimerScreen(
    timerState: TimerState,
    viewModel: MainViewModel,
    onExitFullScreen: () -> Unit,
    onParkDistraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onExitFullScreen()
    }

    val context = LocalContext.current
    val studyStartSoundEnabled by viewModel.studyStartSoundEnabled.collectAsStateWithLifecycle()
    val studyEndSoundEnabled by viewModel.studyEndSoundEnabled.collectAsStateWithLifecycle()
    val studyBreakStartSoundEnabled by viewModel.studyBreakStartSoundEnabled.collectAsStateWithLifecycle()
    val studyBreakEndSoundEnabled by viewModel.studyBreakEndSoundEnabled.collectAsStateWithLifecycle()
    val studyKeepScreenOnEnabled by viewModel.studyKeepScreenOnEnabled.collectAsStateWithLifecycle()
    var showSoundSettingsDialog by remember { mutableStateOf(false) }

    // Screen awake effect: prevents display timeout while focus or break timer is running
    com.example.util.KeepScreenOnEffect(
        enabled = studyKeepScreenOnEnabled,
        isTimerActive = timerState.isRunning
    )

    var showNotesField by remember { mutableStateOf(false) }
    var sessionNotes by remember { mutableStateOf("") }
    var showRecordConfirmation by remember { mutableStateOf(false) }

    val displaySeconds = if (timerState.mode == TimerMode.STOPWATCH) {
        timerState.elapsedSeconds
    } else {
        timerState.remainingSeconds
    }
    val safeSeconds = maxOf(0, displaySeconds)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    val timeFormatted = String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)

    val progress = if (timerState.targetSeconds > 0 && timerState.mode != TimerMode.STOPWATCH) {
        (timerState.elapsedSeconds.toFloat() / timerState.targetSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("fullscreen_study_timer_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Exit button & Task context
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (timerState.isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Text(
                            text = if (timerState.isRunning) "FOCUS ACTIVE" else if (timerState.isPaused) "PAUSED" else "READY",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showSoundSettingsDialog = true },
                        modifier = Modifier.testTag("fullscreen_sound_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Sound Signals",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onExitFullScreen,
                        modifier = Modifier.testTag("exit_fullscreen_timer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit Full-Screen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Focus Body: Circular Progress Ring & High-Contrast Digits
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.widthIn(max = 480.dp)
            ) {
                // Task Title & Category Badge
                Text(
                    text = timerState.selectedTaskTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "${timerState.selectedCategory} • ${timerState.mode.title}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // Micro-break active banner
                val isBreakActive = timerState.currentPhase == com.example.data.model.TimerPhase.SHORT_BREAK ||
                        timerState.currentPhase == com.example.data.model.TimerPhase.LONG_BREAK
                if (isBreakActive) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🌿 ${timerState.currentPhase.displayName} in Progress",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            val breakRem = maxOf(0, timerState.currentPhaseTargetSeconds - timerState.currentPhaseElapsedSeconds)
                            val bMin = breakRem / 60
                            val bSec = breakRem % 60
                            Text(
                                text = String.format(java.util.Locale.getDefault(), "Break Remaining: %02d:%02d", bMin, bSec),
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.skipBreak() },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("fullscreen_skip_break_button")
                                ) {
                                    Text("Skip Break", style = MaterialTheme.typography.labelSmall)
                                }
                                FilledTonalButton(
                                    onClick = { viewModel.extendBreak(60) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("fullscreen_extend_break_1m_button")
                                ) {
                                    Text("+1m", style = MaterialTheme.typography.labelSmall)
                                }
                                FilledTonalButton(
                                    onClick = { viewModel.extendBreak(300) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("fullscreen_extend_break_5m_button")
                                ) {
                                    Text("+5m", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Circular Progress + Digital Clock
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(260.dp)
                ) {
                    if (timerState.mode != TimerMode.STOPWATCH) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            strokeWidth = 10.dp
                        )
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = if (timerState.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                            strokeWidth = 10.dp
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = timeFormatted,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 56.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = if (timerState.mode == TimerMode.STOPWATCH) "Elapsed Time" else "Remaining Time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Primary Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (timerState.isRunning) {
                        FilledTonalButton(
                            onClick = { viewModel.pauseTimer() },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(52.dp).testTag("fullscreen_pause_button")
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause", modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pause", style = MaterialTheme.typography.titleMedium)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.startTimer(context) },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.height(52.dp).testTag("fullscreen_start_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start", modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (timerState.isPaused) "Resume" else "Start Focus",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = { viewModel.stopTimer() },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(52.dp).testTag("fullscreen_reset_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset")
                    }

                    if (timerState.elapsedSeconds > 30 || timerState.isCompleted) {
                        Button(
                            onClick = { showRecordConfirmation = true },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(52.dp).testTag("fullscreen_finish_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Finish", modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Finish")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Actions: Notes & Thought Parking
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (showNotesField) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            OutlinedTextField(
                                value = sessionNotes,
                                onValueChange = { sessionNotes = it },
                                label = { Text("Session Notes / Reflections") },
                                placeholder = { Text("What did you solve or accomplish?") },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth().testTag("fullscreen_session_notes_input")
                            )
                        }
                    }
                } else {
                    TextButton(
                        onClick = { showNotesField = true },
                        modifier = Modifier.testTag("fullscreen_add_notes_btn")
                    ) {
                        Text("+ Add Session Notes")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onParkDistraction,
                        modifier = Modifier.testTag("fullscreen_park_distraction_btn")
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Park Distraction (Brain Dump)")
                    }

                    TextButton(
                        onClick = onExitFullScreen
                    ) {
                        Text("Exit Full Screen")
                    }
                }
            }
        }
    }

    if (showRecordConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRecordConfirmation = false },
            title = { Text("Finish & Save Session?") },
            text = {
                val mins = maxOf(1, timerState.elapsedSeconds / 60)
                Text("Log this $mins-minute study session for '${timerState.selectedTaskTitle}' to your study history?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCompletedSession(
                            notes = sessionNotes.trim().ifBlank { null },
                            status = "COMPLETED"
                        )
                        showRecordConfirmation = false
                        onExitFullScreen()
                    },
                    modifier = Modifier.testTag("confirm_save_session_button")
                ) {
                    Text("Save & Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecordConfirmation = false }) {
                    Text("Keep Going")
                }
            }
        )
    }

    if (showSoundSettingsDialog) {
        StudyTimerSoundSettingsDialog(
            keepScreenOnEnabled = studyKeepScreenOnEnabled,
            onToggleKeepScreenOn = { viewModel.setStudyKeepScreenOn(it) },
            startSoundEnabled = studyStartSoundEnabled,
            breakStartSoundEnabled = studyBreakStartSoundEnabled,
            breakEndSoundEnabled = studyBreakEndSoundEnabled,
            endSoundEnabled = studyEndSoundEnabled,
            onToggleStartSound = { viewModel.setStudyStartSoundEnabled(it) },
            onToggleBreakStartSound = { viewModel.setStudyBreakStartSoundEnabled(it) },
            onToggleBreakEndSound = { viewModel.setStudyBreakEndSoundEnabled(it) },
            onToggleEndSound = { viewModel.setStudyEndSoundEnabled(it) },
            onPreviewSound = { type -> com.example.util.StudySoundHelper.previewSound(context, type) },
            onDismiss = { showSoundSettingsDialog = false }
        )
    }
}
