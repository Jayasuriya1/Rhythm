package com.example.ui.screens.study

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.style.TextOverflow
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

/**
 * 100% Immersive Full-Screen Focus Timer & Ambient Room Experience.
 *
 * Requirements:
 * - 100% viewport coverage (completely overlays navigation rail and mobile navigation bar)
 * - Zero scroll: responsive fit on phone portrait, phone landscape (split), and tablets
 * - OLED dark aesthetic (#000000 / near-black, muted sage/mint accents, soft white text)
 * - Hierarchical structure: Top Controls, Center Focus Circle, Bottom Controls
 * - Park a Thought instant modal (adds to Brain Dump without stopping the timer)
 * - In-session Notes and calm Completion Experience
 * - Background controls: OLED pure black, 10 curated atmospheric backdrops, or user gallery photo
 */
@Composable
fun FullScreenFocusTimerScreen(
    timerState: TimerState,
    viewModel: MainViewModel,
    currentTheme: FocusAestheticTheme = FocusAestheticTheme.MINIMAL_OLED,
    onSelectTheme: (FocusAestheticTheme) -> Unit = {},
    onExitFullScreen: () -> Unit,
    onParkDistraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onExitFullScreen()
    }

    val context = LocalContext.current

    // True Immersive Fullscreen Mode
    val studyImmersiveFullscreenEnabled by viewModel.studyImmersiveFullscreenEnabled.collectAsStateWithLifecycle()
    com.example.util.ImmersiveFocusEffect(enabled = studyImmersiveFullscreenEnabled)

    // Screen awake management
    val studyKeepScreenOnEnabled by viewModel.studyKeepScreenOnEnabled.collectAsStateWithLifecycle()
    com.example.util.KeepScreenOnEffect(
        enabled = studyKeepScreenOnEnabled,
        isTimerActive = timerState.isRunning
    )

    // Audio & Soundscape States
    val activeSoundscape by ConcentrationSoundscapeHelper.currentSoundscape.collectAsStateWithLifecycle()
    val isSoundscapePlaying by ConcentrationSoundscapeHelper.isPlaying.collectAsStateWithLifecycle()
    val soundscapeVolume by ConcentrationSoundscapeHelper.volume.collectAsStateWithLifecycle()
    val musicTitle by StudyMusicPlayer.currentTrackTitle.collectAsStateWithLifecycle()
    val isMusicPlaying by StudyMusicPlayer.isPlaying.collectAsStateWithLifecycle()

    // Background Presentation Preferences from ViewModel
    val bgType by viewModel.focusBgType.collectAsStateWithLifecycle()
    val bgBuiltinId by viewModel.focusBgBuiltinId.collectAsStateWithLifecycle()
    val customUri by viewModel.focusBgCustomUri.collectAsStateWithLifecycle()
    val wallpapers by com.example.util.FocusWallpaperManager.wallpapers.collectAsStateWithLifecycle()
    val isSlideshow by viewModel.focusSlideshowEnabled.collectAsStateWithLifecycle()
    val slideshowIntervalMins by viewModel.focusSlideshowIntervalMinutes.collectAsStateWithLifecycle()
    val overlayOpacity by viewModel.focusBgOverlayOpacity.collectAsStateWithLifecycle()
    val brightness by viewModel.focusBgBrightness.collectAsStateWithLifecycle()
    val isBlur by viewModel.focusBgBlur.collectAsStateWithLifecycle()
    val fitMode by viewModel.focusBgFit.collectAsStateWithLifecycle()
    val isAmbientAnimation by viewModel.focusAmbientAnimationEnabled.collectAsStateWithLifecycle()

    var slideshowIndex by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(isSlideshow, wallpapers, slideshowIntervalMins) {
        if (isSlideshow && wallpapers.isNotEmpty()) {
            val intervalMillis = (slideshowIntervalMins.coerceAtLeast(1) * 60 * 1000L)
            while (true) {
                kotlinx.coroutines.delay(intervalMillis)
                slideshowIndex = (slideshowIndex + 1) % wallpapers.size
            }
        }
    }

    val activeCustomUri = remember(bgType, customUri, isSlideshow, wallpapers, slideshowIndex) {
        if (bgType == FocusBgType.CUSTOM) {
            if (isSlideshow && wallpapers.isNotEmpty()) {
                wallpapers.getOrNull(slideshowIndex % wallpapers.size)?.filePath ?: customUri
            } else {
                customUri
            }
        } else {
            null
        }
    }

    val effectiveTheme = remember(bgBuiltinId) {
        FocusAestheticTheme.fromId(bgBuiltinId)
    }

    // Dialog States
    var showStudioDialog by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var showParkThoughtModal by remember { mutableStateOf(false) }
    var showEndSessionConfirmation by remember { mutableStateOf(false) }
    var showVolumePopover by remember { mutableStateOf(false) }
    var sessionNotes by remember { mutableStateOf("") }
    var parkedThoughtText by remember { mutableStateOf("") }

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

    // If session is completed, display the calm completion experience
    val isSessionComplete = timerState.isCompleted || timerState.currentPhase == TimerPhase.COMPLETED

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("fullscreen_study_timer_screen")
    ) {
        val isWideScreen = maxWidth >= 600.dp
        val isLandscape = maxWidth > maxHeight
        val isShortWindow = maxHeight < 560.dp

        // 1. Ambient Background Layer
        FocusBackgroundLayer(
            bgType = bgType,
            builtinTheme = effectiveTheme,
            customImageUri = activeCustomUri,
            overlayOpacity = overlayOpacity,
            brightness = brightness,
            isBlurEnabled = isBlur,
            scaleMode = fitMode,
            isAmbientAnimationEnabled = isAmbientAnimation,
            isTimerRunning = timerState.isRunning
        )

        // 2. Main Content
        if (isSessionComplete) {
            // Calm Completion Experience
            CalmCompletionExperience(
                intention = timerState.selectedTaskTitle,
                elapsedSeconds = timerState.elapsedSeconds,
                sessionNotes = sessionNotes,
                onNotesChange = { sessionNotes = it },
                onTakeBreak = {
                    viewModel.extendBreak(300) // 5-minute break
                },
                onStartAnotherSession = {
                    viewModel.stopTimer()
                    viewModel.startTimer(context)
                },
                onSaveAndExit = {
                    viewModel.saveCompletedSession(
                        notes = sessionNotes.trim().ifBlank { null },
                        status = "COMPLETED"
                    )
                    onExitFullScreen()
                }
            )
        } else if (isLandscape && !isWideScreen) {
            // Landscape Phone Split Layout (Avoids vertical crowding & prevents scrolling)
            LandscapePhoneFocusView(
                timerState = timerState,
                timeFormatted = timeFormatted,
                animatedProgress = animatedProgress,
                accentColor = effectiveTheme.primaryAccent,
                isBreakActive = isBreakActive,
                activeBreak = activeBreak,
                breakEffect = breakEffect,
                safeSeconds = safeSeconds,
                activeSoundscape = activeSoundscape,
                isSoundscapePlaying = isSoundscapePlaying,
                sessionNotes = sessionNotes,
                onStart = {
                    viewModel.startTimer(context)
                    StudyMusicPlayer.onStudyTimerStarted()
                    if (activeSoundscape != SoundscapeType.NONE && !isSoundscapePlaying) {
                        ConcentrationSoundscapeHelper.playCurrent()
                    }
                },
                onPause = {
                    viewModel.pauseTimer()
                    StudyMusicPlayer.onStudyTimerPaused()
                },
                onEndSession = {
                    if (timerState.isRunning && timerState.elapsedSeconds > 30) {
                        showEndSessionConfirmation = true
                    } else {
                        viewModel.stopTimer()
                        onExitFullScreen()
                    }
                },
                onOpenNotes = { showNotesDialog = true },
                onParkThought = { showParkThoughtModal = true },
                onOpenStudio = { showStudioDialog = true },
                onToggleVolume = { showVolumePopover = !showVolumePopover },
                onExit = onExitFullScreen,
                onSkipBreak = { viewModel.skipBreak() },
                onExtendBreak = { viewModel.extendBreak(it) }
            )
        } else {
            // Portrait Phone & All Tablet View (Proportional, non-scrolling centered stack)
            PortraitAndTabletFocusView(
                timerState = timerState,
                timeFormatted = timeFormatted,
                animatedProgress = animatedProgress,
                accentColor = effectiveTheme.primaryAccent,
                isWideScreen = isWideScreen,
                isShortWindow = isShortWindow,
                isBreakActive = isBreakActive,
                activeBreak = activeBreak,
                breakEffect = breakEffect,
                safeSeconds = safeSeconds,
                activeSoundscape = activeSoundscape,
                isSoundscapePlaying = isSoundscapePlaying,
                musicTitle = musicTitle,
                isMusicPlaying = isMusicPlaying,
                sessionNotes = sessionNotes,
                onStart = {
                    viewModel.startTimer(context)
                    StudyMusicPlayer.onStudyTimerStarted()
                    if (activeSoundscape != SoundscapeType.NONE && !isSoundscapePlaying) {
                        ConcentrationSoundscapeHelper.playCurrent()
                    }
                },
                onPause = {
                    viewModel.pauseTimer()
                    StudyMusicPlayer.onStudyTimerPaused()
                },
                onEndSession = {
                    if (timerState.isRunning && timerState.elapsedSeconds > 30) {
                        showEndSessionConfirmation = true
                    } else {
                        viewModel.stopTimer()
                        onExitFullScreen()
                    }
                },
                onOpenNotes = { showNotesDialog = true },
                onParkThought = { showParkThoughtModal = true },
                onOpenStudio = { showStudioDialog = true },
                onToggleVolume = { showVolumePopover = !showVolumePopover },
                onExit = onExitFullScreen,
                onSkipBreak = { viewModel.skipBreak() },
                onExtendBreak = { viewModel.extendBreak(it) }
            )
        }

        // Quick Volume Popover Overlay
        if (showVolumePopover) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showVolumePopover = false }
                    .padding(top = 70.dp, end = 24.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141C16)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF233226)),
                    modifier = Modifier
                        .width(240.dp)
                        .clickable(enabled = false) {}
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Soundscape Volume",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA8C9B0)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Slider(
                                value = soundscapeVolume,
                                onValueChange = { ConcentrationSoundscapeHelper.setVolume(it) },
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFA8C9B0),
                                    activeTrackColor = Color(0xFFA8C9B0),
                                    inactiveTrackColor = Color(0xFF243026)
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // In-Session Notes Editor Modal
    if (showNotesDialog) {
        AlertDialog(
            onDismissRequest = { showNotesDialog = false },
            containerColor = Color(0xFF0F1411),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFE2E8E4),
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notes, contentDescription = null, tint = Color(0xFFA8C9B0), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Study Session Notes")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Jot down solutions, insights, or code references during your session:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8F9E94)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = sessionNotes,
                        onValueChange = { sessionNotes = it },
                        placeholder = { Text("What did you solve or learn?", color = Color(0xFF556358)) },
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFA8C9B0),
                            unfocusedBorderColor = Color(0xFF263329),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color(0xFFE2E8E4)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showNotesDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA8C9B0), contentColor = Color(0xFF08140B))
                ) {
                    Text("Save Notes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotesDialog = false }) {
                    Text("Close", color = Color(0xFF8F9E94))
                }
            }
        )
    }

    // Park a Thought (Brain Dump Fast Capture Modal)
    if (showParkThoughtModal) {
        AlertDialog(
            onDismissRequest = { showParkThoughtModal = false },
            containerColor = Color(0xFF0F1411),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFE2E8E4),
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFA8C9B0), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Park a Thought")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Quickly offload sudden ideas or distractions to Brain Dump without stopping your focus timer:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8F9E94)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = parkedThoughtText,
                        onValueChange = { parkedThoughtText = it },
                        placeholder = { Text("e.g., Pay electric bill, check PR #42...", color = Color(0xFF556358)) },
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFA8C9B0),
                            unfocusedBorderColor = Color(0xFF263329),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color(0xFFE2E8E4)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (parkedThoughtText.isNotBlank()) {
                            viewModel.saveBrainDumpNote(
                                title = "Focus Distraction",
                                content = parkedThoughtText.trim(),
                                tag = "Focus"
                            )
                            parkedThoughtText = ""
                        }
                        showParkThoughtModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA8C9B0), contentColor = Color(0xFF08140B))
                ) {
                    Text("Park & Resume")
                }
            },
            dismissButton = {
                TextButton(onClick = { showParkThoughtModal = false }) {
                    Text("Cancel", color = Color(0xFF8F9E94))
                }
            }
        )
    }

    // End Session Confirmation Dialog
    if (showEndSessionConfirmation) {
        val elapsedMins = maxOf(1, timerState.elapsedSeconds / 60)
        AlertDialog(
            onDismissRequest = { showEndSessionConfirmation = false },
            containerColor = Color(0xFF0F1411),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFE2E8E4),
            shape = RoundedCornerShape(20.dp),
            title = { Text("End Focus Session?") },
            text = {
                Text(
                    "You have focused for $elapsedMins minute${if (elapsedMins > 1) "s" else ""}. Would you like to log this session to your study history or continue focusing?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8F9E94)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCompletedSession(
                            notes = sessionNotes.trim().ifBlank { null },
                            status = "COMPLETED"
                        )
                        showEndSessionConfirmation = false
                        onExitFullScreen()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA8C9B0), contentColor = Color(0xFF08140B))
                ) {
                    Text("End & Log")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndSessionConfirmation = false }) {
                    Text("Keep Focusing", color = Color(0xFF8F9E94))
                }
            }
        )
    }

    // Focus Studio Full Dialog
    if (showStudioDialog) {
        FocusBackdropAudioStudioDialog(
            viewModel = viewModel,
            currentTheme = effectiveTheme,
            onThemeSelected = onSelectTheme,
            onDismiss = { showStudioDialog = false }
        )
    }
}

/**
 * Portrait Phone & Tablet View: Elegantly balanced vertical stack with zero scrolling.
 */
@Composable
private fun PortraitAndTabletFocusView(
    timerState: TimerState,
    timeFormatted: String,
    animatedProgress: Float,
    accentColor: Color,
    isWideScreen: Boolean,
    isShortWindow: Boolean,
    isBreakActive: Boolean,
    activeBreak: com.example.data.model.BreakPoint?,
    breakEffect: String,
    safeSeconds: Int,
    activeSoundscape: SoundscapeType,
    isSoundscapePlaying: Boolean,
    musicTitle: String?,
    isMusicPlaying: Boolean,
    sessionNotes: String,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onEndSession: () -> Unit,
    onOpenNotes: () -> Unit,
    onParkThought: () -> Unit,
    onOpenStudio: () -> Unit,
    onToggleVolume: () -> Unit,
    onExit: () -> Unit,
    onSkipBreak: () -> Unit,
    onExtendBreak: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = if (isWideScreen) 36.dp else 20.dp,
                vertical = if (isShortWindow) 12.dp else 20.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP CONTROLS: Status indicator, Soundscape shortcut, Volume, Studio, Exit
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Indicator Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF131A15).copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF202C23))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = when {
                            isBreakActive -> Color(0xFF60A5FA)
                            timerState.isRunning -> Color(0xFFA8C9B0)
                            timerState.isPaused -> Color(0xFFFBBF24)
                            else -> Color(0xFF6B7A70)
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
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color(0xFFE2E8E4)
                    )
                }
            }

            // Top Quick Action Shortcuts
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                // Soundscape quick indicator
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF131A15).copy(alpha = 0.75f),
                    modifier = Modifier.clickable { onOpenStudio() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Soundscape",
                            tint = if (isSoundscapePlaying || isMusicPlaying) Color(0xFFA8C9B0) else Color(0xFF8F9E94),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (activeSoundscape != SoundscapeType.NONE) activeSoundscape.displayName else if (isMusicPlaying) "Music" else "Studio",
                            fontSize = 11.sp,
                            color = Color(0xFFE2E8E4),
                            maxLines = 1
                        )
                    }
                }

                IconButton(
                    onClick = onToggleVolume,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Volume",
                        tint = Color(0xFF8F9E94),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onOpenStudio,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Focus Studio",
                        tint = Color(0xFF8F9E94),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onExit,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit Focus Mode",
                        tint = Color(0xFF8F9E94),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // CENTER STAGE: Intention + Large Circular Countdown (Fits on screen without scroll)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (isBreakActive) {
                BreakCenterpiece(
                    breakPoint = activeBreak,
                    phase = timerState.currentPhase,
                    timeFormatted = timeFormatted,
                    safeSeconds = safeSeconds,
                    effectStyle = breakEffect,
                    accentColor = accentColor,
                    onSkipBreak = onSkipBreak,
                    onExtendBreak = onExtendBreak
                )
            } else {
                FocusTimerCenterpiece(
                    timerState = timerState,
                    timeFormatted = timeFormatted,
                    animatedProgress = animatedProgress,
                    accentColor = accentColor,
                    isWideScreen = isWideScreen,
                    isShortWindow = isShortWindow
                )
            }
        }

        // BOTTOM CONTROLS: Primary Pause/Resume, End Session, Park Thought, Notes
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 540.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (timerState.isRunning) {
                    FilledTonalButton(
                        onClick = onPause,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF1E2820),
                            contentColor = Color(0xFFA8C9B0)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("fullscreen_pause_button")
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pause", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                } else {
                    Button(
                        onClick = onStart,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFA8C9B0),
                            contentColor = Color(0xFF08140B)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("fullscreen_start_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Start", modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (timerState.isPaused) "Resume" else "Start Focus",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // End / Log Session Button
                FilledTonalButton(
                    onClick = onEndSession,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF161F18),
                        contentColor = Color(0xFFC0CCC3)
                    ),
                    modifier = Modifier
                        .weight(0.85f)
                        .height(52.dp)
                        .testTag("fullscreen_end_session_button")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "End Session", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("End Session", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Quick Actions: Park a Thought, Session Notes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onParkThought,
                    modifier = Modifier.testTag("fullscreen_park_distraction_btn")
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFA8C9B0), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Park a Thought", fontSize = 12.sp, color = Color(0xFFD4DED7))
                }

                TextButton(
                    onClick = onOpenNotes,
                    modifier = Modifier.testTag("fullscreen_add_notes_btn")
                ) {
                    Icon(Icons.Default.Notes, contentDescription = null, tint = Color(0xFFA8C9B0), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (sessionNotes.isBlank()) "Session Notes" else "Notes (${sessionNotes.length}c)",
                        fontSize = 12.sp,
                        color = Color(0xFFD4DED7)
                    )
                }

                TextButton(onClick = onOpenStudio) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF8F9E94), modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Environment", fontSize = 12.sp, color = Color(0xFF8F9E94))
                }
            }
        }
    }
}

/**
 * Landscape Phone View: Split layout with countdown circle on the left and
 * controls/intention on the right. Guaranteed no vertical scroll.
 */
@Composable
private fun LandscapePhoneFocusView(
    timerState: TimerState,
    timeFormatted: String,
    animatedProgress: Float,
    accentColor: Color,
    isBreakActive: Boolean,
    activeBreak: com.example.data.model.BreakPoint?,
    breakEffect: String,
    safeSeconds: Int,
    activeSoundscape: SoundscapeType,
    isSoundscapePlaying: Boolean,
    sessionNotes: String,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onEndSession: () -> Unit,
    onOpenNotes: () -> Unit,
    onParkThought: () -> Unit,
    onOpenStudio: () -> Unit,
    onToggleVolume: () -> Unit,
    onExit: () -> Unit,
    onSkipBreak: () -> Unit,
    onExtendBreak: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Half: Large Timer Circle
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                if (timerState.mode != TimerMode.STOPWATCH) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = Color.White.copy(alpha = 0.12f),
                        strokeWidth = 7.dp
                    )
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFFA8C9B0),
                        strokeWidth = 7.dp
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 38.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = if (isBreakActive) "Break Time" else if (timerState.mode == TimerMode.STOPWATCH) "Elapsed" else "Remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8F9E94)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Right Half: Task Intention + Action Buttons
        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar in right pane: intention + close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = timerState.selectedTaskTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${timerState.selectedCategory} • ${timerState.mode.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8F9E94)
                    )
                }

                Row {
                    IconButton(onClick = onOpenStudio, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Tune, contentDescription = "Studio", tint = Color(0xFF8F9E94), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onExit, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Exit", tint = Color(0xFF8F9E94), modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Middle: Action Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (timerState.isRunning) {
                    FilledTonalButton(
                        onClick = onPause,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF1E2820), contentColor = Color(0xFFA8C9B0)),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pause", fontSize = 13.sp)
                    }
                } else {
                    Button(
                        onClick = onStart,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA8C9B0), contentColor = Color(0xFF08140B)),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                FilledTonalButton(
                    onClick = onEndSession,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF161F18), contentColor = Color(0xFFC0CCC3)),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("End", fontSize = 13.sp)
                }
            }

            // Bottom bar: Park Thought & Notes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onParkThought) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFA8C9B0), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Park Thought", fontSize = 11.sp, color = Color(0xFFD4DED7))
                }

                TextButton(onClick = onOpenNotes) {
                    Icon(Icons.Default.Notes, contentDescription = null, tint = Color(0xFFA8C9B0), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (sessionNotes.isBlank()) "Notes" else "Notes*", fontSize = 11.sp, color = Color(0xFFD4DED7))
                }
            }
        }
    }
}

/**
 * Centerpiece Focus Circle & Intention Display
 */
@Composable
private fun FocusTimerCenterpiece(
    timerState: TimerState,
    timeFormatted: String,
    animatedProgress: Float,
    accentColor: Color,
    isWideScreen: Boolean,
    isShortWindow: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.widthIn(max = 520.dp)
    ) {
        // Study Intention / Task Name
        Text(
            text = timerState.selectedTaskTitle,
            style = (if (isShortWindow) MaterialTheme.typography.titleMedium else if (isWideScreen) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge)
                .copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Category & Mode Tag
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF141D16).copy(alpha = 0.8f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223226))
        ) {
            Text(
                text = "${timerState.selectedCategory} • ${timerState.mode.title}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = Color(0xFFA8C9B0),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(if (isShortWindow) 10.dp else if (isWideScreen) 24.dp else 16.dp))

        // Large Circular Countdown
        val circleSize = when {
            isShortWindow -> 170.dp
            isWideScreen -> 270.dp
            else -> 230.dp
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(circleSize)
        ) {
            if (timerState.mode != TimerMode.STOPWATCH) {
                // Background Track
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White.copy(alpha = 0.10f),
                    strokeWidth = if (isWideScreen) 10.dp else 8.dp
                )

                // Active Progress
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFA8C9B0),
                    strokeWidth = if (isWideScreen) 10.dp else 8.dp
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
                            isShortWindow -> 36.sp
                            isWideScreen -> 58.sp
                            else -> 48.sp
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
                    color = Color(0xFF8F9E94)
                )
            }
        }
    }
}

/**
 * Break Experience Centerpiece during scheduled rest intervals
 */
@Composable
private fun BreakCenterpiece(
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
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF131D24),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223542)),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = if (phase == TimerPhase.LONG_BREAK) Icons.Default.Coffee else Icons.Default.Spa,
                    contentDescription = null,
                    tint = Color(0xFF60A5FA),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = breakLabel,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF93C5FD),
                    textAlign = TextAlign.Center
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(220.dp)
                .padding(8.dp)
        ) {
            when (effectStyle) {
                "BREATHING_GUIDE" -> BreathingGuideEffect(accentColor)
                "AMBIENT_RIPPLE" -> AmbientRippleEffect(accentColor)
                "ZEN_GLOW" -> ZenGlowEffect(accentColor)
                else -> MinimalBreakEffect(accentColor)
            }

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
                    color = Color(0xFFBAC7BF)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onSkipBreak,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334437)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Skip Break", fontSize = 12.sp)
            }

            FilledTonalButton(
                onClick = { onExtendBreak(60) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF162019), contentColor = Color(0xFFA8C9B0))
            ) {
                Text("+1m", fontSize = 12.sp)
            }

            FilledTonalButton(
                onClick = { onExtendBreak(300) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF162019), contentColor = Color(0xFFA8C9B0))
            ) {
                Text("+5m", fontSize = 12.sp)
            }
        }
    }
}

/**
 * Calm Completion Experience:
 * "You showed up. That matters."
 * Calm reflection, duration confirmation, optional notes, and next actions.
 */
@Composable
private fun CalmCompletionExperience(
    intention: String,
    elapsedSeconds: Int,
    sessionNotes: String,
    onNotesChange: (String) -> Unit,
    onTakeBreak: () -> Unit,
    onStartAnotherSession: () -> Unit,
    onSaveAndExit: () -> Unit
) {
    val mins = maxOf(1, elapsedSeconds / 60)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Icon & Affirmation
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 520.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF16241A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF283F2F)),
                modifier = Modifier.size(68.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFFA8C9B0),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Focus Session Completed",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "“You showed up. That matters.”",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                ),
                color = Color(0xFFA8C9B0)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF131A15),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF202C23))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = intention,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "$mins minutes of undisturbed deep focus",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8F9E94)
                    )
                }
            }
        }

        // Center: Session Reflection Notes
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
        ) {
            Text(
                text = "What did you learn or accomplish? (Optional)",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF8F9E94),
                modifier = Modifier.padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = sessionNotes,
                onValueChange = onNotesChange,
                placeholder = { Text("Summarize your progress, solved bugs, or takeaways...", color = Color(0xFF556358)) },
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFA8C9B0),
                    unfocusedBorderColor = Color(0xFF233226),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color(0xFFE2E8E4)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            )
        }

        // Bottom Actions: Take Break, Start Another Session, Save & Exit
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onSaveAndExit,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFA8C9B0),
                    contentColor = Color(0xFF08140B)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Save Session & Return to Study", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onTakeBreak,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF162019),
                        contentColor = Color(0xFF93C5FD)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Coffee, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Take 5m Break")
                }

                FilledTonalButton(
                    onClick = onStartAnotherSession,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF162019),
                        contentColor = Color(0xFFA8C9B0)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start Another")
                }
            }
        }
    }
}

// ------------------------------------------------------------------------
// Ambient Break Visual Effects (Breathing Guide, Ripples, Zen Glow, Minimal)
// ------------------------------------------------------------------------
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

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFA8C9B0).copy(alpha = 0.35f), Color.Transparent),
                center = center,
                radius = baseRadius * 1.3f
            ),
            radius = baseRadius * 1.3f,
            center = center
        )

        drawCircle(
            color = Color(0xFFA8C9B0).copy(alpha = 0.5f),
            radius = baseRadius,
            center = center,
            style = Stroke(width = 4.dp.toPx())
        )
    }
}

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
                color = Color(0xFFA8C9B0).copy(alpha = alpha),
                radius = radius,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}

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
                    Color(0xFFA8C9B0).copy(alpha = 0.6f),
                    Color(0xFF818CF8).copy(alpha = 0.3f),
                    Color(0xFF38BDF8).copy(alpha = 0.5f),
                    Color(0xFFA8C9B0).copy(alpha = 0.6f)
                ),
                center = center
            ),
            radius = radius,
            center = center,
            style = Stroke(width = 6.dp.toPx())
        )
    }
}

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
            color = Color(0xFFA8C9B0),
            radius = radius,
            center = center,
            style = Stroke(width = 4.dp.toPx())
        )
    }
}
