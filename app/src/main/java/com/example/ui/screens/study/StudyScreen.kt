package com.example.ui.screens.study

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val tasks by viewModel.studyTasks.collectAsStateWithLifecycle()
    val dailyPriority by viewModel.dailyPriorityTask.collectAsStateWithLifecycle()
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    val recentSessions by viewModel.recentStudySessions.collectAsStateWithLifecycle()

    var selectedCategoryFilter by remember { mutableStateOf("All") }
    val categories = listOf("All", "DSA", "JavaScript", "React", "Backend", "Interview Prep", "General")

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskForStuckHelper by remember { mutableStateOf<StudyTaskEntity?>(null) }
    var showSessionCompleteDialog by remember { mutableStateOf(false) }

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
                onStart = { viewModel.startTimer() },
                onPause = { viewModel.pauseTimer() },
                onReset = { viewModel.stopTimer() },
                onModeChange = { mode, customMins -> viewModel.setTimerConfig(mode, customMins) },
                onCompleteSession = { showSessionCompleteDialog = true }
            )
        }

        // Daily Priority Section
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

        // Category Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategoryFilter == cat,
                        onClick = { selectedCategoryFilter = cat },
                        label = { Text(cat) }
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
            items(filteredTasks, key = { it.id }) { task ->
                StudyTaskItemCard(
                    task = task,
                    onStatusChange = { newStatus -> viewModel.updateTaskStatus(task, newStatus) },
                    onTogglePriority = { viewModel.toggleDailyPriority(task) },
                    onSelectForTimer = { viewModel.selectTaskForTimer(task) },
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

            items(recentSessions.take(5), key = { it.id }) { session ->
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
                        Column {
                            Text(
                                text = session.taskTitle,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${session.category} • ${DateTimeUtils.formatDisplayDate(session.date)}",
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

    // Add Task Dialog
    if (showAddTaskDialog) {
        AddStudyTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, cat, mins, priority ->
                viewModel.createStudyTask(title, cat, mins, priority)
                showAddTaskDialog = false
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

    // Complete Session Dialog
    if (showSessionCompleteDialog) {
        RecordSessionDialog(
            timerState = timerState,
            onDismiss = { showSessionCompleteDialog = false },
            onConfirm = { notes ->
                viewModel.saveCompletedSession(notes)
                showSessionCompleteDialog = false
            }
        )
    }
}

@Composable
fun FocusTimerCard(
    timerState: com.example.ui.TimerState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onModeChange: (TimerMode, Int) -> Unit,
    onCompleteSession: () -> Unit
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
                        modifier = Modifier.clickable { onModeChange(mode, mode.defaultMinutes) }
                    ) {
                        Text(
                            text = when (mode) {
                                TimerMode.POMODORO -> "25m"
                                TimerMode.DEEP_WORK -> "50m"
                                TimerMode.CUSTOM -> "30m"
                                TimerMode.STOPWATCH -> "Stopwatch"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                        text = "Focus: ${timerState.selectedTaskTitle}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Large Digital Timer Readout
            val displaySeconds = if (timerState.mode == TimerMode.STOPWATCH) {
                timerState.elapsedSeconds
            } else {
                maxOf(0, timerState.targetSeconds - timerState.elapsedSeconds)
            }
            val minutes = displaySeconds / 60
            val seconds = displaySeconds % 60
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

                if (timerState.elapsedSeconds > 30) {
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
        }
    }
}

@Composable
fun StudyTaskItemCard(
    task: StudyTaskEntity,
    onStatusChange: (String) -> Unit,
    onTogglePriority: () -> Unit,
    onSelectForTimer: () -> Unit,
    onStuckClick: () -> Unit,
    onDelete: () -> Unit
) {
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
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    text = task.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
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

                StatusBadge(status = task.status)
            }

            // Show broken down small step if user used "I'm stuck"
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
                            Icons.Default.HelpOutline,
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

            // Actions row: Timer, I'm Stuck, Status
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
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("DSA") }
    var estimatedMinutes by remember { mutableIntStateOf(30) }
    var isPriority by remember { mutableStateOf(false) }

    val categories = listOf("DSA", "JavaScript", "React", "Backend", "Interview Prep", "General")

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
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
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
                        onConfirm(title, category, estimatedMinutes, isPriority)
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
    timerState: com.example.ui.TimerState,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var learnedNotes by remember { mutableStateOf("") }
    val durationMin = maxOf(1, timerState.elapsedSeconds / 60)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Study Session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Logged Duration: $durationMin minutes",
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
