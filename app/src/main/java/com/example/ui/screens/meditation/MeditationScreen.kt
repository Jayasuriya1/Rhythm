package com.example.ui.screens.meditation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NaturePeople
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Slider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.MeditationSessionEntity
import com.example.ui.MainViewModel
import com.example.util.DateTimeUtils
import com.example.util.MeditationAudioHelper
import com.example.util.MeditationAudioMode
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MeditationTechnique(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val defaultMinutes: Int,
    val icon: ImageVector,
    val guidanceSteps: List<String>
)

val MEDITATION_TECHNIQUES = listOf(
    MeditationTechnique(
        id = "mindful_breathing",
        title = "Mindful Breathing",
        subtitle = "Gentle, natural breath pacing",
        description = "Settle comfortably. Follow each natural inhale and exhale without forcing or straining your breath.",
        defaultMinutes = 5,
        icon = Icons.Default.Air,
        guidanceSteps = listOf(
            "Find a relaxed, upright posture. Soften your jaw and shoulders.",
            "Rest your awareness at the tip of your nose or the gentle rise and fall of your belly.",
            "Breathe naturally. When your mind wanders, gently notice and bring your attention back with kindness."
        )
    ),
    MeditationTechnique(
        id = "body_scan",
        title = "Gentle Body Scan",
        subtitle = "Releasing held tension from head to toe",
        description = "Gradually move your awareness through the body, noting sensations and releasing tension without judgment.",
        defaultMinutes = 8,
        icon = Icons.Default.SelfImprovement,
        guidanceSteps = listOf(
            "Bring attention to your feet and toes. Feel the contact with the ground or cushion.",
            "Gently shift awareness up into calves, knees, and thighs. Allow muscles to soften.",
            "Notice your lower back and abdomen. Breathe softly into any tightness.",
            "Release shoulders, neck, and throat. Unclench teeth and let your forehead smooth.",
            "Rest in the open sensation of the entire body breathing as one."
        )
    ),
    MeditationTechnique(
        id = "grounding_senses",
        title = "Grounding Senses (5-4-3-2-1)",
        subtitle = "Anchoring awareness in your immediate space",
        description = "Engage your five senses sequentially to ground attention firmly in the present moment.",
        defaultMinutes = 5,
        icon = Icons.Default.NaturePeople,
        guidanceSteps = listOf(
            "5: Notice 5 things you can see around you right now.",
            "4: Notice 4 physical textures you can feel (clothes, floor, breeze).",
            "3: Notice 3 sounds in your environment (near or distant).",
            "2: Notice 2 subtle scents or aromas in the air.",
            "1: Notice 1 comforting thought or sensation you are grateful for."
        )
    ),
    MeditationTechnique(
        id = "mindful_observation",
        title = "Mindful Observation",
        subtitle = "Observing thoughts like passing clouds",
        description = "Watch arising thoughts and feelings without clinging or reacting, allowing them to pass freely.",
        defaultMinutes = 5,
        icon = Icons.Default.Visibility,
        guidanceSteps = listOf(
            "Imagine your mind as an expansive, clear sky.",
            "When thoughts or urges appear, observe them like passing clouds in the atmosphere.",
            "You do not need to push thoughts away or hold onto them. Simply watch them drift through."
        )
    ),
    MeditationTechnique(
        id = "study_break",
        title = "Study Break Reset",
        subtitle = "Quick refresh for tired eyes and mind",
        description = "Relieve screen fatigue, unclench physical tension, and take three centering breaths before continuing work.",
        defaultMinutes = 3,
        icon = Icons.Default.Spa,
        guidanceSteps = listOf(
            "Look away from screens toward a distant focal point across the room or outside.",
            "Roll your shoulders backwards three times and gently stretch your wrists.",
            "Inhale deeply through your nose, pause for a moment, and exhale with a soft sigh.",
            "Set a calm, clear intention for your next focused study block."
        )
    )
)

/**
 * Phase 6 — Guided Meditation Module.
 * Offline, fully respectful of user agency, no streak pressure, with clear medical disclaimer.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MeditationScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val recentSessions by viewModel.recentMeditationSessions.collectAsStateWithLifecycle()

    var selectedTechnique by remember { mutableStateOf(MEDITATION_TECHNIQUES[0]) }
    var sessionDurationMinutes by remember { mutableIntStateOf(selectedTechnique.defaultMinutes) }

    var isSessionRunning by remember { mutableStateOf(false) }
    var isSessionPaused by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var showBreathingAnimation by remember { mutableStateOf(true) }

    var showCompleteDialog by remember { mutableStateOf(false) }
    var reflectionNotes by remember { mutableStateOf("") }

    val context = LocalContext.current
    val audioHelper = remember { MeditationAudioHelper(context) }

    val persistedAudioMode by viewModel.meditationAudioMode.collectAsStateWithLifecycle()
    val persistedInhaleSec by viewModel.meditationInhaleSeconds.collectAsStateWithLifecycle()
    val persistedHoldInhaleSec by viewModel.meditationHoldInhaleSeconds.collectAsStateWithLifecycle()
    val persistedExhaleSec by viewModel.meditationExhaleSeconds.collectAsStateWithLifecycle()
    val persistedHoldExhaleSec by viewModel.meditationHoldExhaleSeconds.collectAsStateWithLifecycle()

    var audioMode by remember { mutableStateOf(persistedAudioMode) }
    var inhaleSeconds by remember { mutableIntStateOf(persistedInhaleSec) }
    var holdInhaleSeconds by remember { mutableIntStateOf(persistedHoldInhaleSec) }
    var exhaleSeconds by remember { mutableIntStateOf(persistedExhaleSec) }
    var holdExhaleSeconds by remember { mutableIntStateOf(persistedHoldExhaleSec) }

    LaunchedEffect(persistedAudioMode) { audioMode = persistedAudioMode }
    LaunchedEffect(persistedInhaleSec) { inhaleSeconds = persistedInhaleSec }
    LaunchedEffect(persistedHoldInhaleSec) { holdInhaleSeconds = persistedHoldInhaleSec }
    LaunchedEffect(persistedExhaleSec) { exhaleSeconds = persistedExhaleSec }
    LaunchedEffect(persistedHoldExhaleSec) { holdExhaleSeconds = persistedHoldExhaleSec }

    var voiceGuidanceEnabled by remember { mutableStateOf(true) }
    var speechRate by remember { mutableFloatStateOf(0.85f) }
    var selectedAudioUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var selectedAudioName by remember { mutableStateOf<String?>(null) }
    var currentSpokenStep by remember { mutableIntStateOf(-1) }
    var audioPositionMs by remember { mutableIntStateOf(0) }
    var audioDurationMs by remember { mutableIntStateOf(0) }
    var voicePreviewStatus by remember { mutableStateOf<String?>(null) }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            selectedAudioUri = uri
            var name: String? = null
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIdx)
                    }
                }
            } catch (_: Exception) {}
            selectedAudioName = name ?: "Custom Meditation Audio"
        }
    }

    LaunchedEffect(audioHelper) {
        audioHelper.onStepSpoken = { idx, _ ->
            currentSpokenStep = idx
        }
        audioHelper.onAudioProgress = { pos, dur ->
            audioPositionMs = pos
            audioDurationMs = dur
        }
        audioHelper.onError = { err ->
            voicePreviewStatus = err
        }
        audioHelper.onSessionComplete = {
            if (isSessionRunning) {
                isSessionRunning = false
                isSessionPaused = false
                showCompleteDialog = true
            }
        }
    }

    DisposableEffect(audioHelper) {
        onDispose {
            audioHelper.release()
        }
    }

    // Session timer ticker loop
    LaunchedEffect(isSessionRunning, isSessionPaused) {
        if (isSessionRunning && !isSessionPaused) {
            while (true) {
                delay(1000L)
                elapsedSeconds++
                val targetSeconds = sessionDurationMinutes * 60
                if (elapsedSeconds >= targetSeconds) {
                    isSessionRunning = false
                    isSessionPaused = false
                    audioHelper.stop()
                    currentSpokenStep = -1
                    showCompleteDialog = true
                    break
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("meditation_screen_list"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Medical & Non-Intervention Disclaimer
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Mindfulness exercises support relaxation and mental clarity. They are not intended as clinical therapy, medical treatment, or prevention of psychiatric episodes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Active Session Card or Technique Selector
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSessionRunning) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth().testTag("active_meditation_card")
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
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = selectedTechnique.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(
                                text = selectedTechnique.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = selectedTechnique.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "${sessionDurationMinutes}m",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Breathing Guide Animation with Configurable Pacing and Cues
                    if (showBreathingAnimation) {
                        BreathingGuideCircle(
                            isRunning = isSessionRunning && !isSessionPaused,
                            inhaleSeconds = inhaleSeconds,
                            holdInhaleSeconds = holdInhaleSeconds,
                            exhaleSeconds = exhaleSeconds,
                            holdExhaleSeconds = holdExhaleSeconds,
                            onPhaseTransition = { phase ->
                                if (voiceGuidanceEnabled && audioMode == MeditationAudioMode.BREATHING_CUES && isSessionRunning && !isSessionPaused) {
                                    when (phase) {
                                        ActiveBreathingPhase.INHALE -> audioHelper.playInhaleCue()
                                        ActiveBreathingPhase.HOLD_IN -> audioHelper.playHoldCue()
                                        ActiveBreathingPhase.EXHALE -> audioHelper.playExhaleCue()
                                        ActiveBreathingPhase.HOLD_OUT -> audioHelper.playHoldCue()
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Large Digital Timer Readout
                    val remainingSeconds = maxOf(0, sessionDurationMinutes * 60 - elapsedSeconds)
                    val mins = (if (isSessionRunning || isSessionPaused) remainingSeconds else sessionDurationMinutes * 60) / 60
                    val secs = (if (isSessionRunning || isSessionPaused) remainingSeconds else sessionDurationMinutes * 60) % 60
                    val timeStr = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Controls Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isSessionRunning && !isSessionPaused) {
                            Button(
                                onClick = {
                                    elapsedSeconds = 0
                                    isSessionRunning = true
                                    isSessionPaused = false

                                    if (voiceGuidanceEnabled) {
                                        when (audioMode) {
                                            MeditationAudioMode.TTS -> {
                                                audioHelper.setSpeechRate(speechRate)
                                                val stepInterval = maxOf(6, (sessionDurationMinutes * 60) / maxOf(1, selectedTechnique.guidanceSteps.size))
                                                audioHelper.startTtsSession(selectedTechnique.guidanceSteps, stepInterval)
                                            }
                                            MeditationAudioMode.BREATHING_CUES -> {
                                                audioHelper.playInhaleCue()
                                            }
                                            MeditationAudioMode.AMBIENT -> {
                                                if (selectedAudioUri != null) {
                                                    audioHelper.startAudioFileSession(selectedAudioUri!!)
                                                } else {
                                                    audioHelper.startSyntheticAmbientDrone()
                                                }
                                            }
                                            MeditationAudioMode.SILENT -> {
                                                // Silent visual pacing
                                            }
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("start_meditation_btn")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Begin Session")
                            }
                        } else if (isSessionRunning && !isSessionPaused) {
                            FilledTonalButton(
                                onClick = {
                                    isSessionPaused = true
                                    audioHelper.pause()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("pause_meditation_btn")
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pause")
                            }

                            OutlinedButton(
                                onClick = {
                                    if (elapsedSeconds > 30) {
                                        showCompleteDialog = true
                                    }
                                    isSessionRunning = false
                                    isSessionPaused = false
                                    audioHelper.stop()
                                    currentSpokenStep = -1
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("finish_meditation_early_btn")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("End")
                            }
                        } else {
                            Button(
                                onClick = {
                                    isSessionPaused = false
                                    if (voiceGuidanceEnabled) {
                                        audioHelper.resume()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("resume_meditation_btn")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Resume")
                            }

                            OutlinedButton(
                                onClick = {
                                    if (elapsedSeconds > 30) {
                                        showCompleteDialog = true
                                    }
                                    isSessionRunning = false
                                    isSessionPaused = false
                                    audioHelper.stop()
                                    currentSpokenStep = -1
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("End")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Breathing Animation Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Visual breathing rhythm guide",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = showBreathingAnimation,
                            onCheckedChange = { showBreathingAnimation = it }
                        )
                    }
                }
            }
        }

        // Mindfulness Guidance & Breathing Rhythm Settings
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth().testTag("voice_guided_meditation_card")
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Guidance Mode",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // 4 Selectable Audio Modes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MeditationAudioMode.values().forEach { mode ->
                            val isSelected = audioMode == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    audioMode = mode
                                    viewModel.setMeditationAudioMode(mode)
                                },
                                label = { Text(mode.displayName, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Mode Details & Previews
                    when (audioMode) {
                        MeditationAudioMode.TTS -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Voice Pacing & Rate:",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf(
                                        0.75f to "0.75x Slow / Mindful",
                                        0.90f to "0.9x Calm",
                                        1.0f to "1.0x Normal"
                                    ).forEach { (rate, label) ->
                                        FilterChip(
                                            selected = speechRate == rate,
                                            onClick = {
                                                speechRate = rate
                                                audioHelper.setSpeechRate(rate)
                                            },
                                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            audioHelper.setSpeechRate(speechRate)
                                            audioHelper.previewVoice("Take a gentle breath in, and let your shoulders drop as you breathe out.")
                                            voicePreviewStatus = "Playing voice preview..."
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("preview_meditation_voice_btn")
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Preview Voice")
                                    }

                                    voicePreviewStatus?.let { status ->
                                        Text(
                                            text = status,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                        MeditationAudioMode.BREATHING_CUES -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Gentle Acoustic Breathing Cues (Rising Inhale, Chime Hold, Soft Exhale):",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedButton(
                                        onClick = { audioHelper.playInhaleCue() },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Inhale Cue", style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedButton(
                                        onClick = { audioHelper.playHoldCue() },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Hold Cue", style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedButton(
                                        onClick = { audioHelper.playExhaleCue() },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Exhale Cue", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                        MeditationAudioMode.AMBIENT -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Calming Background Sound (Local Track or Built-in Binaural Harmonic Drone):",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(
                                    onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("select_meditation_audio_file_btn")
                                ) {
                                    Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(selectedAudioName ?: "Use Local Audio File (.mp3/.wav)")
                                }
                                if (selectedAudioName == null) {
                                    Text(
                                        text = "No custom file chosen: built-in soothing 108Hz/432Hz ambient harmonic drone will play.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        MeditationAudioMode.SILENT -> {
                            Text(
                                text = "Completely silent visual pacing. The glowing breathing circle will pace your inhalation, retention, and exhalation without emitting sound.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Configurable Breathing Rhythm Pacing Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth().testTag("breathing_pacing_card")
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Breathing Rhythm & Pacing",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Presets Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Box (4-4-4-4)" to listOf(4, 4, 4, 4),
                            "4-7-8 Relax" to listOf(4, 7, 8, 0),
                            "Calm (5-2-6)" to listOf(5, 2, 6, 0),
                            "Natural (3-0-3)" to listOf(3, 0, 3, 0)
                        ).forEach { (presetName, values) ->
                            val isPresetMatch = inhaleSeconds == values[0] && holdInhaleSeconds == values[1] &&
                                    exhaleSeconds == values[2] && holdExhaleSeconds == values[3]
                            FilterChip(
                                selected = isPresetMatch,
                                onClick = {
                                    inhaleSeconds = values[0]
                                    holdInhaleSeconds = values[1]
                                    exhaleSeconds = values[2]
                                    holdExhaleSeconds = values[3]
                                    viewModel.setMeditationBreathingPacing(values[0], values[1], values[2], values[3])
                                },
                                label = { Text(presetName, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    // Sliders
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Inhale Duration", style = MaterialTheme.typography.bodySmall)
                            Text("${inhaleSeconds}s", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Slider(
                            value = inhaleSeconds.toFloat(),
                            onValueChange = {
                                inhaleSeconds = it.toInt()
                                viewModel.setMeditationBreathingPacing(inhaleSeconds, holdInhaleSeconds, exhaleSeconds, holdExhaleSeconds)
                            },
                            valueRange = 2f..10f,
                            steps = 7
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Hold After Inhale", style = MaterialTheme.typography.bodySmall)
                            Text("${holdInhaleSeconds}s", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Slider(
                            value = holdInhaleSeconds.toFloat(),
                            onValueChange = {
                                holdInhaleSeconds = it.toInt()
                                viewModel.setMeditationBreathingPacing(inhaleSeconds, holdInhaleSeconds, exhaleSeconds, holdExhaleSeconds)
                            },
                            valueRange = 0f..10f,
                            steps = 9
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Exhale Duration", style = MaterialTheme.typography.bodySmall)
                            Text("${exhaleSeconds}s", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Slider(
                            value = exhaleSeconds.toFloat(),
                            onValueChange = {
                                exhaleSeconds = it.toInt()
                                viewModel.setMeditationBreathingPacing(inhaleSeconds, holdInhaleSeconds, exhaleSeconds, holdExhaleSeconds)
                            },
                            valueRange = 2f..10f,
                            steps = 7
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Hold After Exhale / Rest", style = MaterialTheme.typography.bodySmall)
                            Text("${holdExhaleSeconds}s", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Slider(
                            value = holdExhaleSeconds.toFloat(),
                            onValueChange = {
                                holdExhaleSeconds = it.toInt()
                                viewModel.setMeditationBreathingPacing(inhaleSeconds, holdInhaleSeconds, exhaleSeconds, holdExhaleSeconds)
                            },
                            valueRange = 0f..10f,
                            steps = 9
                        )
                    }
                }
            }
        }

        // Technique Guidance Prompts
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Guidance: ${selectedTechnique.title}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = selectedTechnique.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    selectedTechnique.guidanceSteps.forEachIndexed { idx, step ->
                        val isCurrentlySpoken = currentSpokenStep == idx
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isCurrentlySpoken) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(6.dp)).padding(4.dp)
                                    else Modifier
                                )
                        ) {
                            Text(
                                text = if (isCurrentlySpoken) "▶" else "•",
                                color = if (isCurrentlySpoken) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = step,
                                style = if (isCurrentlySpoken) MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold) else MaterialTheme.typography.bodySmall,
                                color = if (isCurrentlySpoken) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Select Other Techniques
        item {
            Text(
                text = "Meditation Techniques",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MEDITATION_TECHNIQUES.forEach { tech ->
                    val isSelected = tech.id == selectedTechnique.id
                    Card(
                        onClick = {
                            if (!isSessionRunning) {
                                selectedTechnique = tech
                                sessionDurationMinutes = tech.defaultMinutes
                                elapsedSeconds = 0
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("technique_card_${tech.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = tech.icon,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tech.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = tech.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${tech.defaultMinutes} min",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Recent Completed Sessions History
        item {
            Text(
                text = "Recent Practice (${recentSessions.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Practice is for yourself; no streaks or daily pressure required.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (recentSessions.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "No recorded sessions yet. Begin whenever you feel ready.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(recentSessions.take(5), key = { "med_${it.id}" }) { session ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val dateStr = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(session.completedAt))
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!session.reflectionNotes.isNullOrBlank()) {
                                Text(
                                    text = "Reflection: ${session.reflectionNotes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "${session.durationMinutes} min",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    // Completion / Reflection Dialog
    if (showCompleteDialog) {
        val completedDurationMins = maxOf(1, elapsedSeconds / 60)
        AlertDialog(
            onDismissRequest = {
                showCompleteDialog = false
                elapsedSeconds = 0
            },
            title = { Text("Session Complete") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("You completed $completedDurationMins minute(s) of ${selectedTechnique.title}.")
                    OutlinedTextField(
                        value = reflectionNotes,
                        onValueChange = { reflectionNotes = it },
                        label = { Text("Private Reflection (Optional)") },
                        placeholder = { Text("How does your body and mind feel now?") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth().testTag("reflection_notes_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveMeditationSession(
                            techniqueId = selectedTechnique.id,
                            title = selectedTechnique.title,
                            durationMinutes = completedDurationMins,
                            reflectionNotes = reflectionNotes
                        )
                        audioHelper.stop()
                        currentSpokenStep = -1
                        showCompleteDialog = false
                        reflectionNotes = ""
                        elapsedSeconds = 0
                    },
                    modifier = Modifier.testTag("save_meditation_session_btn")
                ) {
                    Text("Save to History")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        audioHelper.stop()
                        currentSpokenStep = -1
                        showCompleteDialog = false
                        reflectionNotes = ""
                        elapsedSeconds = 0
                    }
                ) {
                    Text("Skip Saving")
                }
            }
        )
    }
}

enum class ActiveBreathingPhase(val label: String) {
    INHALE("Inhale softly…"),
    HOLD_IN("Hold gently…"),
    EXHALE("Exhale slowly…"),
    HOLD_OUT("Rest & pause…")
}

/**
 * Calm, comfortable breathing animation circle.
 * Fully configurable pacing for Inhale, Hold, Exhale, and Rest.
 * Smoothly drives visual scale and notifies audio helper on phase transitions.
 */
@Composable
fun BreathingGuideCircle(
    isRunning: Boolean,
    inhaleSeconds: Int = 4,
    holdInhaleSeconds: Int = 4,
    exhaleSeconds: Int = 4,
    holdExhaleSeconds: Int = 0,
    onPhaseTransition: ((ActiveBreathingPhase) -> Unit)? = null
) {
    var currentPhase by remember { mutableStateOf(ActiveBreathingPhase.INHALE) }
    var targetScale by remember { mutableFloatStateOf(0.75f) }
    var animDurationMs by remember { mutableIntStateOf(inhaleSeconds * 1000) }

    LaunchedEffect(isRunning, inhaleSeconds, holdInhaleSeconds, exhaleSeconds, holdExhaleSeconds) {
        if (isRunning) {
            while (true) {
                // Inhale
                currentPhase = ActiveBreathingPhase.INHALE
                onPhaseTransition?.invoke(ActiveBreathingPhase.INHALE)
                targetScale = 1.15f
                animDurationMs = maxOf(1, inhaleSeconds) * 1000
                delay(animDurationMs.toLong())

                // Hold In
                if (holdInhaleSeconds > 0) {
                    currentPhase = ActiveBreathingPhase.HOLD_IN
                    onPhaseTransition?.invoke(ActiveBreathingPhase.HOLD_IN)
                    targetScale = 1.15f
                    animDurationMs = holdInhaleSeconds * 1000
                    delay(animDurationMs.toLong())
                }

                // Exhale
                currentPhase = ActiveBreathingPhase.EXHALE
                onPhaseTransition?.invoke(ActiveBreathingPhase.EXHALE)
                targetScale = 0.75f
                animDurationMs = maxOf(1, exhaleSeconds) * 1000
                delay(animDurationMs.toLong())

                // Hold Out
                if (holdExhaleSeconds > 0) {
                    currentPhase = ActiveBreathingPhase.HOLD_OUT
                    onPhaseTransition?.invoke(ActiveBreathingPhase.HOLD_OUT)
                    targetScale = 0.75f
                    animDurationMs = holdExhaleSeconds * 1000
                    delay(animDurationMs.toLong())
                }
            }
        } else {
            targetScale = 0.9f
            currentPhase = ActiveBreathingPhase.INHALE
        }
    }

    val animatedScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = if (isRunning) animDurationMs else 600, easing = FastOutSlowInEasing),
        label = "breathing_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(140.dp)
                .scale(animatedScale)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxSize()
            ) {}
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                modifier = Modifier.size(90.dp)
            ) {}
            Icon(
                imageVector = Icons.Default.Spa,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isRunning) currentPhase.label else "Ready to breathe",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.primary
        )
    }
}
