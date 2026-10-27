package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.RhythmTopBar
import com.example.ui.screens.health.HealthScreen
import com.example.ui.screens.insights.InsightsScreen
import com.example.ui.screens.routine.RoutineScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.study.StudyScreen
import com.example.ui.screens.today.TodayScreen
import com.example.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class NavigationItem(
    val destination: AppDestination,
    val icon: ImageVector,
    val label: String
)

@Composable
fun RhythmFocusApp(viewModel: MainViewModel) {
    val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val userFeedbackMessage by viewModel.userFeedbackMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userFeedbackMessage) {
        userFeedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    // Handle system back navigation to return to TODAY tab
    BackHandler(enabled = currentDestination != AppDestination.TODAY) {
        viewModel.navigateTo(AppDestination.TODAY)
    }

    var showGlobalQuickCapture by remember { mutableStateOf(false) }

    val navItems = listOf(
        NavigationItem(AppDestination.TODAY, Icons.Default.Today, "Today"),
        NavigationItem(AppDestination.ROUTINE, Icons.Default.Schedule, "Routine"),
        NavigationItem(AppDestination.HEALTH, Icons.Default.SelfImprovement, "Health"),
        NavigationItem(AppDestination.STUDY, Icons.Default.Psychology, "Study"),
        NavigationItem(AppDestination.MEDITATION, Icons.Default.Spa, "Meditation"),
        NavigationItem(AppDestination.INSIGHTS, Icons.Default.Analytics, "Insights")
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        if (isExpandedScreen) {
            // Tablet & Landscape Layout: Side NavigationRail + Main Content Area
            Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("tablet_navigation_rail")
                ) {
                    navItems.forEach { item ->
                        val isSelected = currentDestination == item.destination
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { viewModel.navigateTo(item.destination) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            modifier = Modifier.testTag("nav_rail_${item.label.lowercase()}")
                        )
                    }
                    NavigationRailItem(
                        selected = currentDestination == AppDestination.BRAIN_DUMP,
                        onClick = { viewModel.navigateTo(AppDestination.BRAIN_DUMP) },
                        icon = { Icon(Icons.Default.Psychology, contentDescription = "Brain Dump") },
                        label = { Text("Brain Dump") },
                        modifier = Modifier.testTag("nav_rail_braindump")
                    )
                    NavigationRailItem(
                        selected = currentDestination == AppDestination.REMINDERS,
                        onClick = { viewModel.navigateTo(AppDestination.REMINDERS) },
                        icon = { Icon(Icons.Default.NotificationsActive, contentDescription = "Reminders") },
                        label = { Text("Reminders") },
                        modifier = Modifier.testTag("nav_rail_reminders")
                    )
                    NavigationRailItem(
                        selected = currentDestination == AppDestination.ALARMS,
                        onClick = { viewModel.navigateTo(AppDestination.ALARMS) },
                        icon = { Icon(Icons.Default.Alarm, contentDescription = "Alarms") },
                        label = { Text("Alarms") },
                        modifier = Modifier.testTag("nav_rail_alarms")
                    )
                    NavigationRailItem(
                        selected = currentDestination == AppDestination.SETTINGS,
                        onClick = { viewModel.navigateTo(AppDestination.SETTINGS) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        modifier = Modifier.testTag("nav_rail_settings")
                    )
                }

                Scaffold(
                    topBar = {
                        RhythmTopBar(
                            title = currentDestination.label,
                            selectedDate = selectedDate,
                            onPreviousDay = {
                                val prevDate = adjustDate(selectedDate, -1)
                                viewModel.setSelectedDate(prevDate)
                            },
                            onNextDay = {
                                val nextDate = adjustDate(selectedDate, 1)
                                viewModel.setSelectedDate(nextDate)
                            },
                            onTodayClick = { viewModel.selectToday() },
                            onSettingsClick = { viewModel.navigateTo(AppDestination.SETTINGS) },
                            onBrainDumpClick = { showGlobalQuickCapture = true },
                            onRemindersClick = { viewModel.navigateTo(AppDestination.REMINDERS) },
                            onAlarmsClick = { viewModel.navigateTo(AppDestination.ALARMS) }
                        )
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        AppScreenContent(
                            destination = currentDestination,
                            viewModel = viewModel,
                            isExpandedScreen = true
                        )
                    }
                }
            }
        } else {
            // Phone / Compact Layout: TopBar + Content + Bottom NavigationBar
            Scaffold(
                topBar = {
                    RhythmTopBar(
                        title = currentDestination.label,
                        selectedDate = selectedDate,
                        onPreviousDay = {
                            val prevDate = adjustDate(selectedDate, -1)
                            viewModel.setSelectedDate(prevDate)
                        },
                        onNextDay = {
                            val nextDate = adjustDate(selectedDate, 1)
                            viewModel.setSelectedDate(nextDate)
                        },
                        onTodayClick = { viewModel.selectToday() },
                        onSettingsClick = { viewModel.navigateTo(AppDestination.SETTINGS) },
                        onBrainDumpClick = { showGlobalQuickCapture = true },
                        onRemindersClick = { viewModel.navigateTo(AppDestination.REMINDERS) },
                        onAlarmsClick = { viewModel.navigateTo(AppDestination.ALARMS) }
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("bottom_navigation_bar")
                    ) {
                        navItems.forEach { item ->
                            val isSelected = currentDestination == item.destination
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { viewModel.navigateTo(item.destination) },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag("bottom_nav_${item.label.lowercase()}")
                            )
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets.safeDrawing
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    AppScreenContent(
                        destination = currentDestination,
                        viewModel = viewModel,
                        isExpandedScreen = false
                    )
                }
            }
        }
    }

    if (showGlobalQuickCapture) {
        com.example.ui.screens.braindump.QuickCaptureDialog(
            noteToEdit = null,
            onDismiss = { showGlobalQuickCapture = false },
            onSave = { id, title, content, tag, isPinned ->
                viewModel.saveBrainDumpNote(id, title, content, tag, isPinned)
                showGlobalQuickCapture = false
            },
            onConvertToTaskDirectly = { taskTitle, tag, _ ->
                viewModel.createStudyTask(taskTitle, if (tag == "Study") "General" else tag, 25, false)
                showGlobalQuickCapture = false
            }
        )
    }
}

@Composable
fun AppScreenContent(
    destination: AppDestination,
    viewModel: MainViewModel,
    isExpandedScreen: Boolean
) {
    when (destination) {
        AppDestination.TODAY -> TodayScreen(viewModel = viewModel, isExpandedScreen = isExpandedScreen)
        AppDestination.ROUTINE -> RoutineScreen(viewModel = viewModel)
        AppDestination.HEALTH -> HealthScreen(viewModel = viewModel)
        AppDestination.STUDY -> StudyScreen(viewModel = viewModel)
        AppDestination.MEDITATION -> com.example.ui.screens.meditation.MeditationScreen(viewModel = viewModel)
        AppDestination.BRAIN_DUMP -> com.example.ui.screens.braindump.BrainDumpScreen(viewModel = viewModel)
        AppDestination.REMINDERS -> com.example.ui.screens.reminders.ReminderCenterScreen(viewModel = viewModel)
        AppDestination.ALARMS -> com.example.ui.screens.alarm.AlarmSectionScreen(viewModel = viewModel)
        AppDestination.INSIGHTS -> InsightsScreen(viewModel = viewModel)
        AppDestination.SETTINGS -> SettingsScreen(viewModel = viewModel)
    }
}

private fun adjustDate(dateStr: String, offsetDays: Int): String {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = format.parse(dateStr) ?: Date()
        val cal = Calendar.getInstance().apply {
            time = date
            add(Calendar.DAY_OF_YEAR, offsetDays)
        }
        format.format(cal.time)
    } catch (_: Exception) {
        dateStr
    }
}
