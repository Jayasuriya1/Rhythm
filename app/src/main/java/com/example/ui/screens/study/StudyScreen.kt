package com.example.ui.screens.study

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.StudyCategoryEntity
import com.example.data.entity.StudySessionEntity
import com.example.data.entity.StudyTaskEntity
import com.example.ui.MainViewModel
import com.example.ui.TimerMode
import com.example.ui.components.CalmEmptyState
import com.example.ui.components.StatusBadge
import com.example.util.DateTimeUtils

@Composable
fun StudyScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tasks by viewModel.studyTasks.collectAsStateWithLifecycle()
    val dailyPriority by viewModel.dailyPriorityTask.collectAsStateWithLifecycle()
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    val recentSessions by viewModel.recentStudySessions.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val activeCategories by viewModel.activeCategories.collectAsStateWithLifecycle()
    val studyStartSoundEnabled by viewModel.studyStartSoundEnabled.collectAsStateWithLifecycle()
    val studyEndSoundEnabled by viewModel.studyEndSoundEnabled.collectAsStateWithLifecycle()
    val studyBreakStartSoundEnabled by viewModel.studyBreakStartSoundEnabled.collectAsStateWithLifecycle()
    val studyBreakEndSoundEnabled by viewModel.studyBreakEndSoundEnabled.collectAsStateWithLifecycle()
    val studyKeepScreenOnEnabled by viewModel.studyKeepScreenOnEnabled.collectAsStateWithLifecycle()
    val studyProtocol by viewModel.studyProtocol.collectAsStateWithLifecycle()

    var selectedCategoryFilter by remember { mutableStateOf("All") }

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<StudyTaskEntity?>(null) }
    var taskForStuckHelper by remember { mutableStateOf<StudyTaskEntity?>(null) }
    var showCategoryManagerDialog by remember { mutableStateOf(false) }
    var showCustomTimerDurationDialog by remember { mutableStateOf(false) }
    var showProtocolConfigDialog by remember { mutableStateOf(false) }
    var showSessionCompleteDialog by remember { mutableStateOf(false) }
    var showInterruptedSessionDialog by remember { mutableStateOf(false) }
    var showBrainDumpCaptureDialog by remember { mutableStateOf(false) }
    var showSoundSettingsDialog by remember { mutableStateOf(false) }

    // Screen awake effect: prevents display sleep during active focus when enabled
    com.example.util.KeepScreenOnEffect(
        enabled = studyKeepScreenOnEnabled,
        isTimerActive = timerState.isRunning
    )

    // Auto-trigger session completion dialog safely when timer finishes countdown
    androidx.compose.runtime.LaunchedEffect(timerState.isCompleted) {
        if (timerState.isCompleted) {
            if (!showSessionCompleteDialog) {
                showSessionCompleteDialog = true
            }
        }
    }

    var isFullScreenFocusActive by remember { mutableStateOf(false) }

    if (isFullScreenFocusActive) {
        FullScreenFocusTimerScreen(
            timerState = timerState,
            viewModel = viewModel,
            onExitFullScreen = { isFullScreenFocusActive = false },
            onParkDistraction = { showBrainDumpCaptureDialog = true }
        )
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Focus Timer Card
            item {
                FocusTimerCard(
                    timerState = timerState,
                    onStart = { viewModel.startTimer(context) },
                    onPause = { viewModel.pauseTimer() },
                    onReset = {
                        if (timerState.elapsedSeconds > 60) {
                            showInterruptedSessionDialog = true
                        } else {
                            viewModel.stopTimer()
                        }
                    },
                    onModeChange = { mode, defaultMins ->
                        if (mode == TimerMode.CUSTOM) {
                            showCustomTimerDurationDialog = true
                        } else if (mode == TimerMode.PROTOCOL) {
                            viewModel.setTimerConfig(mode, defaultMins)
                            showProtocolConfigDialog = true
                        } else {
                            viewModel.setTimerConfig(mode, defaultMins)
                        }
                    },
                    onCompleteSession = { showSessionCompleteDialog = true },
                    onParkDistraction = { showBrainDumpCaptureDialog = true },
                    onOpenFullScreen = { isFullScreenFocusActive = true },
                    onOpenSoundSettings = { showSoundSettingsDialog = true },
                    onOpenProtocolConfig = { showProtocolConfigDialog = true },
                    onSkipBreak = { viewModel.skipBreak() },
                    onExtendBreak = { seconds -> viewModel.extendBreak(seconds) }
                )
            }

        // Section Header with Add Task and Manage Categories
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Study Priority & Tasks",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { showCategoryManagerDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("manage_categories_button")
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Categories", style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                        onClick = { showAddTaskDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("add_study_task_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Task")
                    }
                }
            }
        }

        // Category Filter Chips
        item {
            val filterOptions = listOf("All") + activeCategories.map { it.name }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filterOptions) { catName ->
                    val isSelected = selectedCategoryFilter == catName
                    val catEntity = activeCategories.firstOrNull { it.name == catName }
                    val colorMarker = catEntity?.colorHex?.let { parseHexColor(it) } ?: MaterialTheme.colorScheme.primary

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategoryFilter = catName },
                        label = { Text(catName) },
                        leadingIcon = if (catName != "All") {
                            {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(colorMarker)
                                )
                            }
                        } else null
                    )
                }
            }
        }

        // Filtered Tasks List
        val filteredTasks = tasks.filter {
            if (selectedCategoryFilter == "All") true else it.category.equals(selectedCategoryFilter, ignoreCase = true)
        }

        if (filteredTasks.isEmpty()) {
            item {
                CalmEmptyState(
                    icon = Icons.Default.Psychology,
                    title = "No Tasks in $selectedCategoryFilter",
                    message = "Add your primary study priority or smaller practice tasks without pressure.",
                    actionLabel = "Add Study Task",
                    onActionClick = { showAddTaskDialog = true }
                )
            }
        } else {
            items(filteredTasks, key = { "task_${it.id}" }) { task ->
                val categoryEntity = categories.firstOrNull { it.name.equals(task.category, ignoreCase = true) }

                StudyTaskItemCard(
                    task = task,
                    categoryEntity = categoryEntity,
                    onStatusChange = { newStatus -> viewModel.updateTaskStatus(task, newStatus) },
                    onTogglePriority = { viewModel.toggleDailyPriority(task) },
                    onSelectForTimer = {
                        viewModel.selectTaskForTimer(task)
                        viewModel.startTimer()
                    },
                    onEditTask = { taskToEdit = task },
                    onStuckClick = { taskForStuckHelper = task },
                    onDelete = { viewModel.deleteStudyTask(task.id) }
                )
            }
        }

        // Recent Study Sessions Section
        if (recentSessions.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Study Sessions (${recentSessions.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(recentSessions.take(5), key = { "session_${it.id}" }) { session ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.taskTitle,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${session.category} • ${DateTimeUtils.formatDisplayDate(session.date)} • ${session.completionStatus}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!session.learnedNotes.isNullOrBlank()) {
                                Text(
                                    text = "Notes: ${session.learnedNotes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }

                        Text(
                            text = "${session.durationMinutes} min",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(28.dp)) }
    }
    }

    // Add Task Dialog
    if (showAddTaskDialog) {
        AddStudyTaskDialog(
            activeCategories = activeCategories,
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, cat, mins, priority ->
                viewModel.createStudyTask(title, cat, mins, priority)
                showAddTaskDialog = false
            }
        )
    }

    // Edit Task Dialog
    taskToEdit?.let { task ->
        EditStudyTaskDialog(
            task = task,
            activeCategories = activeCategories,
            onDismiss = { taskToEdit = null },
            onConfirm = { id, title, cat, mins, priority ->
                viewModel.editStudyTask(id, title, cat, mins, priority)
                taskToEdit = null
            }
        )
    }

    // Category Manager Dialog
    if (showCategoryManagerDialog) {
        CategoryManagerDialog(
            categories = categories,
            onDismiss = { showCategoryManagerDialog = false },
            onCreateCategory = { name, desc, color, icon ->
                viewModel.createStudyCategory(name, desc, color, icon)
            },
            onUpdateCategory = { cat ->
                viewModel.updateStudyCategory(cat)
            },
            onArchiveCategory = { id, isArchived ->
                viewModel.archiveStudyCategory(id, isArchived)
            },
            onDeleteCategory = { id, name, fallback ->
                viewModel.deleteStudyCategory(id, name, fallback)
            }
        )
    }

    // Custom Timer Duration Dialog
    if (showCustomTimerDurationDialog) {
        CustomTimerDurationDialog(
            onDismiss = { showCustomTimerDurationDialog = false },
            onConfirm = { minutes ->
                viewModel.setTimerConfig(TimerMode.CUSTOM, minutes)
                showCustomTimerDurationDialog = false
            }
        )
    }

    // "I'm Stuck" Small Step Helper Dialog
    taskForStuckHelper?.let { task ->
        ImStuckDialog(
            task = task,
            onDismiss = { taskForStuckHelper = null },
            onSaveSmallStep = { step ->
                viewModel.setTaskSmallStep(task, step)
                taskForStuckHelper = null
            }
        )
    }

    // Completed Session Dialog
    if (showSessionCompleteDialog) {
        RecordSessionDialog(
            title = "Focus Session Completed!",
            timerState = timerState,
            status = "COMPLETED",
            onDismiss = {
                viewModel.stopTimer()
                showSessionCompleteDialog = false
            },
            onConfirm = { notes ->
                viewModel.saveCompletedSession(notes, "COMPLETED")
                showSessionCompleteDialog = false
            }
        )
    }

    // Interrupted / Stopped Early Session Dialog
    if (showInterruptedSessionDialog) {
        RecordSessionDialog(
            title = "Session Paused or Reset",
            timerState = timerState,
            status = "INTERRUPTED",
            onDismiss = {
                viewModel.stopTimer()
                showInterruptedSessionDialog = false
            },
            onConfirm = { notes ->
                viewModel.saveCompletedSession(notes, "INTERRUPTED")
                showInterruptedSessionDialog = false
            }
        )
    }

    // Brain Dump / Park Thought Dialog during study
    if (showBrainDumpCaptureDialog) {
        com.example.ui.screens.braindump.QuickCaptureDialog(
            noteToEdit = null,
            onDismiss = { showBrainDumpCaptureDialog = false },
            onSave = { id, title, content, tag, isPinned ->
                viewModel.saveBrainDumpNote(id, title, content, if (tag == "Idea") "Distraction" else tag, isPinned)
                showBrainDumpCaptureDialog = false
            },
            onConvertToTaskDirectly = { taskTitle, tag, _ ->
                viewModel.createStudyTask(taskTitle, if (tag == "Study") "General" else tag, 25, false)
                showBrainDumpCaptureDialog = false
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

    if (showProtocolConfigDialog) {
        StudyProtocolConfigDialog(
            initialProtocol = studyProtocol,
            onSaveProtocol = { protocol ->
                viewModel.saveStudyProtocol(protocol)
                showProtocolConfigDialog = false
            },
            onDismiss = { showProtocolConfigDialog = false }
        )
    }
}

@Composable
fun StudyTimerSoundSettingsDialog(
    keepScreenOnEnabled: Boolean,
    onToggleKeepScreenOn: (Boolean) -> Unit,
    startSoundEnabled: Boolean,
    breakStartSoundEnabled: Boolean,
    breakEndSoundEnabled: Boolean,
    endSoundEnabled: Boolean,
    onToggleStartSound: (Boolean) -> Unit,
    onToggleBreakStartSound: (Boolean) -> Unit,
    onToggleBreakEndSound: (Boolean) -> Unit,
    onToggleEndSound: (Boolean) -> Unit,
    onPreviewSound: (com.example.util.StudySoundType) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Focus Environment & Signals")
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Text(
                        "Configure display wakefulness and gentle acoustic cues so you can stay in flow without checking the clock.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Keep Screen Awake Card
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Keep Screen Awake During Focus", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                    Text("Prevents display timeout only while a timer is active", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = keepScreenOnEnabled,
                                    onCheckedChange = onToggleKeepScreenOn,
                                    modifier = Modifier.testTag("study_keep_screen_on_switch")
                                )
                            }
                            Text(
                                "Battery impact: Keeping the screen lit increases battery usage. The awake flag clears immediately when paused, stopped, or backgrounded.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                // Sound Cues Header
                item {
                    Text(
                        "Audio Cues",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Start Sound Card
                item {
                    SoundSettingCard(
                        title = "Session Start Sound",
                        description = "Gentle rising two-tone chime when focus begins",
                        enabled = startSoundEnabled,
                        onToggle = onToggleStartSound,
                        onPreview = { onPreviewSound(com.example.util.StudySoundType.SESSION_START) },
                        switchTag = "study_start_sound_switch",
                        buttonTag = "preview_start_sound_button"
                    )
                }

                // Break Start Sound Card
                item {
                    SoundSettingCard(
                        title = "Break Start Sound",
                        description = "Soothing descent cue when it's time to pause and rest",
                        enabled = breakStartSoundEnabled,
                        onToggle = onToggleBreakStartSound,
                        onPreview = { onPreviewSound(com.example.util.StudySoundType.BREAK_START) },
                        switchTag = "study_break_start_sound_switch",
                        buttonTag = "preview_break_start_sound_button"
                    )
                }

                // Break End Sound Card
                item {
                    SoundSettingCard(
                        title = "Break End Sound",
                        description = "Refreshing ascending alert to return to study",
                        enabled = breakEndSoundEnabled,
                        onToggle = onToggleBreakEndSound,
                        onPreview = { onPreviewSound(com.example.util.StudySoundType.BREAK_END) },
                        switchTag = "study_break_end_sound_switch",
                        buttonTag = "preview_break_end_sound_button"
                    )
                }

                // Session End Sound Card
                item {
                    SoundSettingCard(
                        title = "Session Complete Sound",
                        description = "Resonant singing bowl tone when protocol finishes",
                        enabled = endSoundEnabled,
                        onToggle = onToggleEndSound,
                        onPreview = { onPreviewSound(com.example.util.StudySoundType.SESSION_END) },
                        switchTag = "study_end_sound_switch",
                        buttonTag = "preview_end_sound_button"
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun SoundSettingCard(
    title: String,
    description: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onPreview: () -> Unit,
    switchTag: String,
    buttonTag: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag(switchTag)
                )
            }

            OutlinedButton(
                onClick = onPreview,
                modifier = Modifier.fillMaxWidth().testTag(buttonTag)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Preview $title")
            }
        }
    }
}

@Composable
fun FocusTimerCard(
    timerState: com.example.ui.TimerState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onModeChange: (TimerMode, Int) -> Unit,
    onCompleteSession: () -> Unit,
    onParkDistraction: () -> Unit,
    onOpenFullScreen: () -> Unit = {},
    onOpenSoundSettings: () -> Unit = {},
    onOpenProtocolConfig: () -> Unit = {},
    onSkipBreak: () -> Unit = {},
    onExtendBreak: (Int) -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .testTag("focus_timer_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mode Selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                TimerMode.values().forEach { mode ->
                    val isSelected = timerState.mode == mode
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        modifier = Modifier.clickable {
                            if (mode == TimerMode.PROTOCOL && isSelected) {
                                onOpenProtocolConfig()
                            } else {
                                onModeChange(mode, mode.defaultMinutes)
                            }
                        }
                    ) {
                        Text(
                            text = when (mode) {
                                TimerMode.POMODORO -> "25m"
                                TimerMode.DEEP_WORK -> "50m"
                                TimerMode.CUSTOM -> "Custom"
                                TimerMode.PROTOCOL -> "Protocol"
                                TimerMode.STOPWATCH -> "Stopwatch"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (timerState.mode == TimerMode.PROTOCOL) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${timerState.protocol.totalFocusMinutes}m Protocol (${timerState.protocol.scheduleMethod.title})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedButton(
                        onClick = onOpenProtocolConfig,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("configure_protocol_button")
                    ) {
                        Text("Edit Protocol", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Active Task Target
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Focus: ${timerState.selectedTaskTitle} (${timerState.selectedCategory})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Active Micro-Break Banner
            val isBreakActive = timerState.currentPhase == com.example.data.model.TimerPhase.SHORT_BREAK ||
                    timerState.currentPhase == com.example.data.model.TimerPhase.LONG_BREAK
            if (isBreakActive) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
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
                                onClick = onSkipBreak,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("skip_break_button")
                            ) {
                                Text("Skip Break", style = MaterialTheme.typography.labelSmall)
                            }
                            FilledTonalButton(
                                onClick = { onExtendBreak(60) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("extend_break_1m_button")
                            ) {
                                Text("+1m", style = MaterialTheme.typography.labelSmall)
                            }
                            FilledTonalButton(
                                onClick = { onExtendBreak(300) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("extend_break_5m_button")
                            ) {
                                Text("+5m", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // State Badge: Running, Paused, Completed, Idle
            val stateText = when {
                timerState.isCompleted -> "COMPLETED"
                timerState.isRunning -> if (isBreakActive) "RESTING" else "RUNNING"
                timerState.isPaused -> "PAUSED"
                else -> "READY"
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = when {
                    timerState.isCompleted -> MaterialTheme.colorScheme.primary
                    timerState.isRunning -> if (isBreakActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.tertiary
                    timerState.isPaused -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = stateText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = if (timerState.isCompleted || timerState.isRunning) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Large Digital Timer Readout
            val displaySeconds = if (timerState.mode == TimerMode.STOPWATCH) {
                timerState.elapsedSeconds
            } else {
                timerState.remainingSeconds
            }
            val safeSeconds = maxOf(0, displaySeconds)
            val minutes = safeSeconds / 60
            val seconds = safeSeconds % 60
            val timeString = String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)

            Text(
                text = timeString,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Timer Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (timerState.isRunning) {
                    FilledTonalButton(
                        onClick = onPause,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("pause_timer_button")
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pause")
                    }
                } else {
                    Button(
                        onClick = onStart,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("start_timer_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (timerState.isPaused) "Resume" else "Start Focus")
                    }
                }

                FilledTonalButton(
                    onClick = onReset,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("reset_timer_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                }

                if (timerState.elapsedSeconds > 30 || timerState.isCompleted) {
                    Button(
                        onClick = onCompleteSession,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("complete_session_button")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Record")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Record")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onParkDistraction,
                    modifier = Modifier.testTag("park_distraction_button")
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Park Thought", style = MaterialTheme.typography.labelSmall)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onOpenSoundSettings,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("study_sound_settings_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Signals", style = MaterialTheme.typography.labelSmall)
                    }

                    FilledTonalButton(
                        onClick = onOpenFullScreen,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("open_fullscreen_timer_button")
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Full-Screen", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun StudyTaskItemCard(
    task: StudyTaskEntity,
    categoryEntity: StudyCategoryEntity?,
    onStatusChange: (String) -> Unit,
    onTogglePriority: () -> Unit,
    onSelectForTimer: () -> Unit,
    onEditTask: () -> Unit,
    onStuckClick: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = categoryEntity?.colorHex?.let { parseHexColor(it) } ?: MaterialTheme.colorScheme.primary

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isDailyPriority) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        ),
        modifier = Modifier.fillMaxWidth().testTag("study_task_${task.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onTogglePriority,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (task.isDailyPriority) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Daily Priority",
                            tint = if (task.isDailyPriority) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                        )
                    }

                    Column {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(categoryColor)
                                    )
                                    Text(
                                        text = task.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Text(
                                text = "${task.estimatedMinutes}m target",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(status = task.status)
                    IconButton(onClick = onEditTask, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Task", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (!task.nextSmallStep.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Next 5-min step: ${task.nextSmallStep}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onSelectForTimer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Focus", style = MaterialTheme.typography.labelSmall)
                }

                FilledTonalButton(
                    onClick = onStuckClick,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("I'm Stuck", style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = {
                        val nextStatus = when (task.status) {
                            "NOT_STARTED" -> "IN_PROGRESS"
                            "IN_PROGRESS" -> "COMPLETED"
                            else -> "NOT_STARTED"
                        }
                        onStatusChange(nextStatus)
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = when (task.status) {
                            "COMPLETED" -> "Reset"
                            "IN_PROGRESS" -> "Done"
                            else -> "Start"
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
fun AddStudyTaskDialog(
    activeCategories: List<StudyCategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(activeCategories.firstOrNull()?.name ?: "General") }
    var estimatedMinutes by remember { mutableIntStateOf(30) }
    var isPriority by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Study Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    placeholder = { Text("e.g. Solve 2 Two-Pointer Problems") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Category", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(activeCategories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat.name,
                            onClick = { selectedCategory = cat.name },
                            label = { Text(cat.name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Text("Estimated Duration: $estimatedMinutes mins", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 25, 45, 60).forEach { mins ->
                        FilterChip(
                            selected = estimatedMinutes == mins,
                            onClick = { estimatedMinutes = mins },
                            label = { Text("${mins}m") }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable { isPriority = !isPriority }
                ) {
                    Icon(
                        imageVector = if (isPriority) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = null,
                        tint = if (isPriority) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                    )
                    Text("Set as Today's Main Priority", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, selectedCategory, estimatedMinutes, isPriority)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Create Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditStudyTaskDialog(
    task: StudyTaskEntity,
    activeCategories: List<StudyCategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, String, Int, Boolean) -> Unit
) {
    var title by remember(task) { mutableStateOf(task.title) }
    var selectedCategory by remember(task) { mutableStateOf(task.category) }
    var estimatedMinutes by remember(task) { mutableIntStateOf(task.estimatedMinutes) }
    var isPriority by remember(task) { mutableStateOf(task.isDailyPriority) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Study Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Category", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(activeCategories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat.name,
                            onClick = { selectedCategory = cat.name },
                            label = { Text(cat.name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Text("Estimated Duration: $estimatedMinutes mins", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 25, 45, 60, 90).forEach { mins ->
                        FilterChip(
                            selected = estimatedMinutes == mins,
                            onClick = { estimatedMinutes = mins },
                            label = { Text("${mins}m") }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable { isPriority = !isPriority }
                ) {
                    Icon(
                        imageVector = if (isPriority) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = null,
                        tint = if (isPriority) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                    )
                    Text("Daily Main Priority", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(task.id, title, selectedCategory, estimatedMinutes, isPriority)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CategoryManagerDialog(
    categories: List<StudyCategoryEntity>,
    onDismiss: () -> Unit,
    onCreateCategory: (String, String?, String, String) -> Boolean,
    onUpdateCategory: (StudyCategoryEntity) -> Boolean,
    onArchiveCategory: (Long, Boolean) -> Unit,
    onDeleteCategory: (Long, String, String) -> Unit
) {
    var showCreateForm by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<StudyCategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<StudyCategoryEntity?>(null) }

    // Colors available for selection
    val availableColors = listOf(
        "#3F5E48", "#D4A373", "#3B82F6", "#7C3AED", "#EA580C", "#0D9488", "#E11D48", "#6B7280"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Study Categories")
                if (!showCreateForm) {
                    FilledTonalButton(
                        onClick = { showCreateForm = true },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (showCreateForm) {
                    var newName by remember { mutableStateOf("") }
                    var newDesc by remember { mutableStateOf("") }
                    var selectedColor by remember { mutableStateOf(availableColors[0]) }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("New Category", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                label = { Text("Name *") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newDesc,
                                onValueChange = { newDesc = it },
                                label = { Text("Optional Description") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("Color Marker", style = MaterialTheme.typography.labelSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                availableColors.forEach { hex ->
                                    val color = parseHexColor(hex)
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (selectedColor == hex) 2.dp else 0.dp,
                                                color = if (selectedColor == hex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedColor = hex }
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { showCreateForm = false }) { Text("Cancel") }
                                Spacer(modifier = Modifier.width(4.dp))
                                Button(
                                    onClick = {
                                        val success = onCreateCategory(newName, newDesc.ifBlank { null }, selectedColor, "category")
                                        if (success) showCreateForm = false
                                    },
                                    enabled = newName.isNotBlank()
                                ) {
                                    Text("Create")
                                }
                            }
                        }
                    }
                }

                // List of existing categories
                LazyColumn(modifier = Modifier.height(280.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories, key = { "cat_${it.id}_${it.name}" }) { cat ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(parseHexColor(cat.colorHex))
                                    )
                                    Column {
                                        Text(
                                            text = cat.name + if (cat.isArchived) " (Archived)" else "",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (cat.isArchived) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (!cat.description.isNullOrBlank()) {
                                            Text(
                                                text = cat.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    IconButton(onClick = { categoryToEdit = cat }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { onArchiveCategory(cat.id, !cat.isArchived) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (cat.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                            contentDescription = if (cat.isArchived) "Restore" else "Archive",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    // Prevent deleting General fallback directly
                                    if (!cat.name.equals("General", ignoreCase = true)) {
                                        IconButton(onClick = { categoryToDelete = cat }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Done") }
        }
    )

    // Edit Category Dialog
    categoryToEdit?.let { cat ->
        var editName by remember(cat) { mutableStateOf(cat.name) }
        var editDesc by remember(cat) { mutableStateOf(cat.description ?: "") }
        var editColor by remember(cat) { mutableStateOf(cat.colorHex) }

        AlertDialog(
            onDismissRequest = { categoryToEdit = null },
            title = { Text("Edit Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Name *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Color Marker", style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableColors.forEach { hex ->
                            val color = parseHexColor(hex)
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (editColor == hex) 2.dp else 0.dp,
                                        color = if (editColor == hex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { editColor = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = onUpdateCategory(cat.copy(name = editName, description = editDesc.ifBlank { null }, colorHex = editColor))
                        if (success) categoryToEdit = null
                    },
                    enabled = editName.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToEdit = null }) { Text("Cancel") }
            }
        )
    }

    // Delete Confirmation Dialog with Fallback Reassignment
    categoryToDelete?.let { cat ->
        val otherCategories = categories.filter { it.id != cat.id && !it.isArchived }
        var selectedFallback by remember { mutableStateOf(otherCategories.firstOrNull { it.name.equals("General", ignoreCase = true) }?.name ?: "General") }

        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Delete Category '${cat.name}'?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Existing tasks in '${cat.name}' will never be silently deleted. Select a category to reassign them to:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(otherCategories) { fallbackCat ->
                            FilterChip(
                                selected = selectedFallback == fallbackCat.name,
                                onClick = { selectedFallback = fallbackCat.name },
                                label = { Text(fallbackCat.name) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory(cat.id, cat.name, selectedFallback)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reassign & Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun CustomTimerDurationDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedMins by remember { mutableIntStateOf(30) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Focus Interval") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "$selectedMins minutes",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = selectedMins.toFloat(),
                    onValueChange = { selectedMins = it.toInt() },
                    valueRange = 5f..120f,
                    steps = 22
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedMins) }) { Text("Set Duration") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ImStuckDialog(
    task: StudyTaskEntity,
    onDismiss: () -> Unit,
    onSaveSmallStep: (String) -> Unit
) {
    var customStep by remember { mutableStateOf("") }
    val suggestedSteps = listOf(
        "Open IDE and create one blank test file",
        "Write down the problem input and output on paper",
        "Read the problem statement once without coding",
        "Write 3 pseudocode bullet points",
        "Commit and run the focus timer for just 5 minutes"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Overwhelmed or Stuck?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "A big task often triggers procrastination. Turn '${task.title}' into one microscopic 5-minute action to break the paralysis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Pick a gentle starting step:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    suggestedSteps.forEach { step ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSaveSmallStep(step) }
                        ) {
                            Text(
                                text = "• $step",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = customStep,
                    onValueChange = { customStep = it },
                    label = { Text("Or type your own 5-min step") },
                    placeholder = { Text("e.g. Read just page 1") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (customStep.isNotBlank()) onSaveSmallStep(customStep)
                },
                enabled = customStep.isNotBlank()
            ) {
                Text("Save Step")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun RecordSessionDialog(
    title: String,
    timerState: com.example.ui.TimerState,
    status: String,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var learnedNotes by remember { mutableStateOf("") }
    val durationMin = maxOf(1, timerState.elapsedSeconds / 60)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Logged Duration: $durationMin minutes ($status)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Task: ${timerState.selectedTaskTitle} (${timerState.selectedCategory})",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "A completed timer is not proof that you studied; confirm or edit this record to reflect your true effort.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = learnedNotes,
                    onValueChange = { learnedNotes = it },
                    label = { Text("What did you learn / accomplish?") },
                    placeholder = { Text("e.g. Solved two-pointer edge case, learned array partitioning") },
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(learnedNotes.ifBlank { null }) }) {
                Text("Save Session")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Discard") }
        }
    )
}

private fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        Color(0xFF4D6B53)
    }
}
