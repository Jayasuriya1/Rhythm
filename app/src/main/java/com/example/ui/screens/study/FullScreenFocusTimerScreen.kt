package com.example.ui.screens.study

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TimerPhase
import com.example.ui.MainViewModel
import com.example.ui.TimerMode
import com.example.ui.TimerState
import com.example.util.ConcentrationSoundscapeHelper
import com.example.util.SoundscapeType
import com.example.util.StudyMusicPlayer
import kotlin.math.cos
import kotlin.math.sin

/**
 * 100% Immersive Full-Screen Focus Mode & Break Sanctuary.
 *
 * Requirements:
 * - Covers the entire display (overlays tablet NavigationRail & mobile footer)
 * - Zero scroll: perfectly responsive proportional fit on phones & tablets
 * - Custom Named Breaks with live countdown
 * - Animated Visual Break Effects (Breathing Guide Pulse, Ambient Ripple, Zen Glow, Minimal)
 * - Start, pause, resume, reset, finish & notes
 * - Instant escape via BackHandler or close icon
 */
@Composable
fun FullScreenFocusTimerScreen(
    timerState: TimerState,
    viewModel: MainViewModel,
    currentTheme: FocusAestheticTheme = FocusAestheticTheme.ZEN_FOREST,
    onSelectTheme: (FocusAestheticTheme) -> Unit = {},
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
    var showStudioDialog by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var sessionNotes by remember { mutableStateOf("") }
    var showRecordConfirmation by remember { mutableStateOf(false) }

    val activeSoundscape by ConcentrationSoundscapeHelper.currentSoundscape.collectAsStateWithLifecycle()
    val isSoundscapePlaying by ConcentrationSoundscapeHelper.isPlaying.collectAsStateWithLifecycle()
    val musicTitle by StudyMusicPlayer.currentTrackTitle.collectAsStateWithLifecycle()
    val isMusicPlaying by StudyMusicPlayer.isPlaying.collectAsStateWithLifecycle()

    // Screen awake effect: prevents display timeout while timer is active
    com.example.util.KeepScreenOnEffect(
        enabled = studyKeepScreenOnEnabled,
        isTimerActive = timerState.isRunning
    )

    val isBreakActive = timerState.currentPhase == TimerPhase.SHORT_BREAK ||
            timerState.currentPhase == TimerPhase.LONG_BREAK

    val activeBreak = timerState.activeBreakPoint
    val breakEffect = activeBreak?.effectStyle ?: timerState.protocol.breakEffectStyle

    val displaySeconds = if (isBreakActive) {
        val totalBreakTarget = timerState.currentPhaseTargetSeconds + timerState.activeBreakExtensionSeconds
        maxOf(0, totalBreakTarget - timerState.currentPhaseElapsedSeconds)
    } else if (timerState.mode == TimerMode.STOPWATCH) {
        timerState.elapsedSeconds
    } else {
        timerState.remainingSeconds
    }

    val safeSeconds = maxOf(0, displaySeconds)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    val timeFormatted = String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)

    val progress = if (isBreakActive) {
        val totalBreakTarget = timerState.currentPhaseTargetSeconds + timerState.activeBreakExtensionSeconds
        if (totalBreakTarget > 0) (timerState.currentPhaseElapsedSeconds.toFloat() / totalBreakTarget.toFloat()).coerceIn(0f, 1f) else 0f
    } else if (timerState.targetSeconds > 0 && timerState.mode != TimerMode.STOPWATCH) {
        (timerState.elapsedSeconds.toFloat() / timerState.targetSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("fullscreen_study_timer_screen")
    ) {
        val isWideScreen = maxWidth >= 600.dp
        // Compact landscape / short-window layouts need a smaller timer and tighter chrome.
        val isCompactHeight = maxHeight < 620.dp

        // 1. Ambient Animated Background
        AestheticBackdropCanvas(
            theme = currentTheme,
            isTimerRunning = timerState.isRunning
        )

        // 2. Main Content Container (No Scroll — Proportional Fit)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (isCompactHeight) 16.dp else 24.dp,
                    vertical = if (isCompactHeight) 8.dp else 18.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Status, Audio Controls & Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 700.dp),
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
                            color = when {
                                isBreakActive -> Color(0xFF64B5F6)
                                timerState.isRunning -> currentTheme.primaryAccent
                                timerState.isPaused -> Color(0xFFFFB74D)
                                else -> MaterialTheme.colorScheme.outline
                            },
                            modifier = Modifier.size(8.dp)
                        ) {}
                        val badgeText = when {
                            isBreakActive -> if (timerState.currentPhase == TimerPhase.LONG_BREAK) "LONG BREAK" else "MICRO-BREAK"
                            timerState.isRunning -> "FOCUS ACTIVE"
                            timerState.isPaused -> "PAUSED"
                            else -> "READY"
                        }
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showStudioDialog = true },
                        modifier = Modifier.testTag("fullscreen_focus_studio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Focus Studio & Music",
                            tint = if (isSoundscapePlaying || isMusicPlaying) currentTheme.primaryAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

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

            // Center Stage: Either Break Experience (with visual effects) or Focus Ring
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (isBreakActive) {
                    BreakExperienceView(
                        breakPoint = activeBreak,
                        phase = timerState.currentPhase,
                        timeFormatted = timeFormatted,
                        safeSeconds = safeSeconds,
                        effectStyle = breakEffect,
                        accentColor = currentTheme.primaryAccent,
                        onSkipBreak = { viewModel.skipBreak() },
                        onExtendBreak = { secs -> viewModel.extendBreak(secs) }
                    )
                } else {
                    FocusTimerCenterView(
                        timerState = timerState,
                        timeFormatted = timeFormatted,
                        animatedProgress = animatedProgress,
                        accentColor = currentTheme.primaryAccent,
                        isWideScreen = isWideScreen,
                        isCompactHeight = isCompactHeight
                    )
                }
            }

            // Bottom Actions & Controls (Non-scrolling, touch-friendly)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isBreakActive) {
                    // Focus Session Play/Pause/Reset Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val hasRecordButton = timerState.elapsedSeconds > 30 || timerState.isCompleted
                        if (timerState.isRunning) {
                            FilledTonalButton(
                                onClick = {
                                    viewModel.pauseTimer()
                                    StudyMusicPlayer.onStudyTimerPaused()
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(if (isCompactHeight) 46.dp else 52.dp)
                                    .testTag("fullscreen_pause_button")
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = "Pause", modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pause", style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            }
                        } else {
                            Button(
                                onClick = {
                                    viewModel.startTimer(context)
                                    StudyMusicPlayer.onStudyTimerStarted()
                                    if (activeSoundscape != SoundscapeType.NONE && !isSoundscapePlaying) {
                                        ConcentrationSoundscapeHelper.playCurrent()
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(if (isCompactHeight) 46.dp else 52.dp)
                                    .testTag("fullscreen_start_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Start", modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (timerState.isPaused) "Resume" else "Start",
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = {
                                viewModel.stopTimer()
                                StudyMusicPlayer.onStudyTimerPaused()
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(if (hasRecordButton) 0.85f else 1f)
                                .height(if (isCompactHeight) 46.dp else 52.dp)
                                .testTag("fullscreen_reset_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset", style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        }

                        if (hasRecordButton) {
                            Button(
                                onClick = { showRecordConfirmation = true },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(if (isCompactHeight) 46.dp else 52.dp)
                                    .testTag("fullscreen_record_button")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Record", modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Record", style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isCompactHeight) 2.dp else 10.dp))

                // Bottom Meta Bar: Notes, Park Distraction, and Exit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showNotesDialog = true },
                        modifier = Modifier.testTag("fullscreen_add_notes_btn")
                    ) {
                        Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (sessionNotes.isBlank()) "+ Session Notes" else "Edit Notes (${sessionNotes.length}c)", fontSize = 12.sp)
                    }

                    TextButton(
                        onClick = onParkDistraction,
                        modifier = Modifier.testTag("fullscreen_park_distraction_btn")
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Park Distraction", fontSize = 12.sp)
                    }

                    TextButton(onClick = onExitFullScreen) {
                        Text("Exit Focus Mode", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Session Notes Modal Dialog
    if (showNotesDialog) {
        AlertDialog(
            onDismissRequest = { showNotesDialog = false },
            title = { Text("Study Session Notes") },
            text = {
                OutlinedTextField(
                    value = sessionNotes,
                    onValueChange = { sessionNotes = it },
                    placeholder = { Text("What did you achieve, solve, or learn?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    maxLines = 5
                )
            },
            confirmButton = {
                Button(onClick = { showNotesDialog = false }) {
                    Text("Save Notes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotesDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showRecordConfirmation) {
        AlertDialog(
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

    if (showStudioDialog) {
        FocusBackdropAudioStudioDialog(
            currentTheme = currentTheme,
            onThemeSelected = onSelectTheme,
            onDismiss = { showStudioDialog = false }
        )
    }
}

/**
 * Break Experience: Displays the custom break name, calming screen effects,
 * remaining break countdown, and quick extension/skip buttons.
 */
@Composable
private fun BreakExperienceView(
    breakPoint: com.example.data.model.BreakPoint?,
    phase: TimerPhase,
    timeFormatted: String,
    safeSeconds: Int,
    effectStyle: String,
    accentColor: Color,
    onSkipBreak: () -> Unit,
    onExtendBreak: (Int) -> Unit
) {
    val breakLabel = breakPoint?.label ?: if (phase == TimerPhase.LONG_BREAK) "Section Recharge Break" else "Micro-Break"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp)
            .padding(12.dp)
    ) {
        // Break Title Badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = if (phase == TimerPhase.LONG_BREAK) Icons.Default.Coffee else Icons.Default.Spa,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = breakLabel,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Visual Break Screen Effect Centerpiece
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(240.dp)
                .padding(8.dp)
        ) {
            when (effectStyle) {
                "BREATHING_GUIDE" -> BreathingGuideEffect(accentColor)
                "AMBIENT_RIPPLE" -> AmbientRippleEffect(accentColor)
                "ZEN_GLOW" -> ZenGlowEffect(accentColor)
                else -> MinimalBreakEffect(accentColor)
            }

            // High-Contrast Countdown Digits
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = Color.White
                )
                Text(
                    text = if (safeSeconds < 60) "${safeSeconds}s remaining" else "Rest & Step Away",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Break Actions
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onSkipBreak,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                modifier = Modifier.testTag("fullscreen_skip_break_button")
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Skip Break", fontSize = 12.sp)
            }

            FilledTonalButton(
                onClick = { onExtendBreak(30) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("fullscreen_extend_break_30s_button")
            ) {
                Text("+30s", fontSize = 12.sp)
            }

            FilledTonalButton(
                onClick = { onExtendBreak(60) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("fullscreen_extend_break_1m_button")
            ) {
                Text("+1m", fontSize = 12.sp)
            }

            FilledTonalButton(
                onClick = { onExtendBreak(300) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("fullscreen_extend_break_5m_button")
            ) {
                Text("+5m", fontSize = 12.sp)
            }
        }
    }
}

/**
 * Animated Breathing Guide Circle with gentle expanding/contracting rhythm.
 */
@Composable
private fun BreathingGuideEffect(accentColor: Color) {
    val transition = rememberInfiniteTransition(label = "breathing")
    val pulseScale by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = (size.minDimension / 2f) * pulseScale

        // Outer Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accentColor.copy(alpha = 0.35f), Color.Transparent),
                center = center,
                radius = baseRadius * 1.3f
            ),
            radius = baseRadius * 1.3f,
            center = center
        )

        // Inner Circle Ring
        drawCircle(
            color = accentColor.copy(alpha = 0.5f),
            radius = baseRadius,
            center = center,
            style = Stroke(width = 4.dp.toPx())
        )
    }
}

/**
 * Concentric Ambient Ripple Waves Effect
 */
@Composable
private fun AmbientRippleEffect(accentColor: Color) {
    val transition = rememberInfiniteTransition(label = "ripple")
    val rippleProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f

        for (i in 0..2) {
            val progress = (rippleProgress + (i * 0.33f)) % 1f
            val radius = maxRadius * progress
            val alpha = (1f - progress) * 0.4f

            drawCircle(
                color = accentColor.copy(alpha = alpha),
                radius = radius,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}

/**
 * Zen Starlight Ambient Glow Effect
 */
@Composable
private fun ZenGlowEffect(accentColor: Color) {
    val transition = rememberInfiniteTransition(label = "zen")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing)
        ),
        label = "zen_rotation"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2.2f

        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.6f),
                    Color(0xFF818CF8).copy(alpha = 0.3f),
                    Color(0xFF38BDF8).copy(alpha = 0.5f),
                    accentColor.copy(alpha = 0.6f)
                ),
                center = center
            ),
            radius = radius,
            center = center,
            style = Stroke(width = 6.dp.toPx())
        )
    }
}

/**
 * Minimalist Clean Progress Indicator
 */
@Composable
private fun MinimalBreakEffect(accentColor: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2.3f

        drawCircle(
            color = Color.White.copy(alpha = 0.2f),
            radius = radius,
            center = center,
            style = Stroke(width = 4.dp.toPx())
        )
        drawCircle(
            color = accentColor,
            radius = radius,
            center = center,
            style = Stroke(width = 4.dp.toPx())
        )
    }
}

/**
 * Center Focus View during active study sprints
 */
@Composable
private fun FocusTimerCenterView(
    timerState: TimerState,
    timeFormatted: String,
    animatedProgress: Float,
    accentColor: Color,
    isWideScreen: Boolean,
    isCompactHeight: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.widthIn(max = 500.dp)
    ) {
        // Task Title & Category Badge
        Text(
            text = timerState.selectedTaskTitle,
            style = (if (isCompactHeight) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium)
                .copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.15f)
        ) {
            Text(
                text = "${timerState.selectedCategory} • ${timerState.mode.title}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(if (isCompactHeight) 6.dp else 20.dp))

        // Circular Progress Ring & High-Contrast Digits
        val circleSize = when {
            isCompactHeight -> 168.dp
            isWideScreen -> 250.dp
            else -> 220.dp
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(circleSize)
        ) {
            if (timerState.mode != TimerMode.STOPWATCH) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White.copy(alpha = 0.15f),
                    strokeWidth = 8.dp
                )
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = accentColor,
                    strokeWidth = 8.dp
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = when {
                            isCompactHeight -> 38.sp
                            isWideScreen -> 54.sp
                            else -> 46.sp
                        },
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = Color.White
                )

                Text(
                    text = if (timerState.mode == TimerMode.STOPWATCH) "Elapsed Focus" else "Remaining Focus",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }
    }
}
