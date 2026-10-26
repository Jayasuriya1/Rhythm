package com.example.ui.screens.reminders

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.ReminderEntity
import com.example.ui.MainViewModel
import com.example.util.AlarmScheduler

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderCenterScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allReminders by viewModel.allReminders.collectAsStateWithLifecycle()
    val masterEnabled by viewModel.remindersMasterEnabled.collectAsStateWithLifecycle()
    val quietHoursEnabled by viewModel.quietHoursEnabled.collectAsStateWithLifecycle()
    val quietStartMin by viewModel.quietHoursStartMinutes.collectAsStateWithLifecycle()
    val quietEndMin by viewModel.quietHoursEndMinutes.collectAsStateWithLifecycle()
    val quietUrgentBypass by viewModel.quietHoursUrgentBypass.collectAsStateWithLifecycle()

    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var reminderToEdit by remember { mutableStateOf<ReminderEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var reminderToDelete by remember { mutableStateOf<ReminderEntity?>(null) }
    var showQuietHoursDialog by remember { mutableStateOf(false) }

    // Android 13+ Notification Permission Launcher
    var hasNotificationPermission by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    val canExactAlarm = AlarmScheduler.canScheduleExactAlarms(context)

    val filteredReminders = remember(allReminders, selectedCategoryFilter) {
        when (selectedCategoryFilter) {
            "ALL" -> allReminders
            "SLEEP" -> allReminders.filter { it.category in listOf("WAKE_UP", "BEDTIME", "WIND_DOWN") }
            "ROUTINE" -> allReminders.filter { it.category in listOf("ROUTINE_ANCHOR", "MEAL") }
            "MEDICATION" -> allReminders.filter { it.category == "MEDICATION" }
            "HYDRATION_STUDY" -> allReminders.filter { it.category in listOf("HYDRATION", "STUDY_BREAK") }
            else -> allReminders
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("reminder_center_list"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Switch Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (masterEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("master_reminder_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (masterEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (masterEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                        contentDescription = null,
                                        tint = if (masterEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Reminder Center",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${allReminders.size} in DB • ${allReminders.count { it.isEnabled }} enabled • ${if (masterEnabled) allReminders.count { it.isEnabled } else 0} scheduled alarms",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = masterEnabled,
                            onCheckedChange = { viewModel.setRemindersMasterEnabled(it, context) },
                            modifier = Modifier.testTag("master_reminder_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Actions Row
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("add_reminder_fab")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Reminder")
                        }

                        FilledTonalButton(
                            onClick = { viewModel.cleanDuplicateAlarms(context) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("clean_duplicate_alarms_btn")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Deduplicate Alarms")
                        }

                        OutlinedButton(
                            onClick = { viewModel.syncFromRoutines(context) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("sync_routines_btn")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync Routines")
                        }

                        OutlinedButton(
                            onClick = { viewModel.syncFromMedications(context) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("sync_meds_btn")
                        ) {
                            Icon(Icons.Default.Medication, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync Meds")
                        }

                        FilledTonalButton(
                            onClick = { viewModel.testSendNotification(context) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("test_notification_btn")
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Ring")
                        }
                    }
                }
            }
        }

        // Notification Permission Warning if denied on Android 13+
        if (!hasNotificationPermission) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth().testTag("notification_permission_card")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.PriorityHigh, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Notifications are Disabled",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "Enable notification permissions so alarms and rhythm reminders can pop up on time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("enable_notifications_btn")
                        ) {
                            Text("Enable")
                        }
                    }
                }
            }
        }

        // Exact Alarm Warning if disabled on Android 12+
        if (!canExactAlarm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().testTag("exact_alarm_permission_card")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Exact Alarms Recommended",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                "Grant exact alarm access to ensure medication alerts trigger down to the exact minute.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("grant_exact_alarm_btn")
                        ) {
                            Text("Settings")
                        }
                    }
                }
            }
        }

        // Quiet Hours Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth().testTag("quiet_hours_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Nightlight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(
                                    "Quiet Hours",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    "Mutes routine and study notifications during rest",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = quietHoursEnabled,
                            onCheckedChange = { viewModel.setQuietHoursEnabled(it) },
                            modifier = Modifier.testTag("quiet_hours_switch")
                        )
                    }

                    if (quietHoursEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Active Window: ${formatMinutesToAmPm(quietStartMin)} – ${formatMinutesToAmPm(quietEndMin)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.primary
                            )

                            TextButton(
                                onClick = { showQuietHoursDialog = true },
                                modifier = Modifier.testTag("edit_quiet_hours_btn")
                            ) {
                                Text("Edit Times")
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Bypass for Critical Medications",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                )
                                Text(
                                    "Ensures critical medication alerts ring even during quiet hours",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = quietUrgentBypass,
                                onCheckedChange = { viewModel.setQuietHoursUrgentBypass(it) },
                                modifier = Modifier.testTag("quiet_urgent_bypass_switch")
                            )
                        }
                    }
                }
            }
        }

        // Category Filter Chips
        item {
            Text(
                text = "Scheduled Alarms (${filteredReminders.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CategoryFilterChip(
                    label = "All (${allReminders.size})",
                    isSelected = selectedCategoryFilter == "ALL",
                    onClick = { selectedCategoryFilter = "ALL" }
                )
                CategoryFilterChip(
                    label = "Sleep & Bedtime",
                    isSelected = selectedCategoryFilter == "SLEEP",
                    onClick = { selectedCategoryFilter = "SLEEP" }
                )
                CategoryFilterChip(
                    label = "Routine & Meals",
                    isSelected = selectedCategoryFilter == "ROUTINE",
                    onClick = { selectedCategoryFilter = "ROUTINE" }
                )
                CategoryFilterChip(
                    label = "Medications",
                    isSelected = selectedCategoryFilter == "MEDICATION",
                    onClick = { selectedCategoryFilter = "MEDICATION" }
                )
                CategoryFilterChip(
                    label = "Hydration & Study",
                    isSelected = selectedCategoryFilter == "HYDRATION_STUDY",
                    onClick = { selectedCategoryFilter = "HYDRATION_STUDY" }
                )
            }
        }

        // Reminders List
        if (filteredReminders.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No reminders scheduled in this section",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { showAddDialog = true }) {
                            Text("+ Add Reminder")
                        }
                    }
                }
            }
        } else {
            items(filteredReminders, key = { it.id }) { reminder ->
                ReminderItemCard(
                    reminder = reminder,
                    onToggle = { isEnabled -> viewModel.toggleReminderEnabled(reminder, isEnabled, context) },
                    onEdit = { reminderToEdit = reminder },
                    onDelete = { reminderToDelete = reminder }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Add / Edit Dialog
    if (showAddDialog) {
        EditReminderDialog(
            reminder = null,
            onDismiss = { showAddDialog = false },
            onSave = { newReminder ->
                viewModel.saveReminder(newReminder, context)
                showAddDialog = false
            }
        )
    }

    reminderToEdit?.let { rem ->
        EditReminderDialog(
            reminder = rem,
            onDismiss = { reminderToEdit = null },
            onSave = { updated ->
                viewModel.saveReminder(updated, context)
                reminderToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    reminderToDelete?.let { rem ->
        AlertDialog(
            onDismissRequest = { reminderToDelete = null },
            title = { Text("Delete Reminder?") },
            text = { Text("Are you sure you want to delete '${rem.title}' scheduled for ${rem.formattedTime()}?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteReminder(rem, context)
                        reminderToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_reminder_btn")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { reminderToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Quiet Hours Times Dialog
    if (showQuietHoursDialog) {
        QuietHoursScheduleDialog(
            initialStartMinutes = quietStartMin,
            initialEndMinutes = quietEndMin,
            onDismiss = { showQuietHoursDialog = false },
            onConfirm = { sMin, eMin ->
                viewModel.setQuietHoursTime(sMin, eMin)
                showQuietHoursDialog = false
            }
        )
    }
}

@Composable
fun ReminderItemCard(
    reminder: ReminderEntity,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryIcon = getCategoryIcon(reminder.category)
    val categoryColor = getCategoryColor(reminder.category)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.isEnabled) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (reminder.isEnabled) 1.5.dp else 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reminder_card_${reminder.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = categoryColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = categoryIcon,
                                contentDescription = null,
                                tint = categoryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = reminder.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (reminder.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (reminder.isUrgent) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = "Urgent",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (reminder.voiceAnnouncementEnabled) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Voice Announcement Active",
                                            modifier = Modifier.size(11.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = "Voice",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = reminder.customMessage ?: getCategoryLabel(reminder.category),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = reminder.isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("reminder_switch_${reminder.id}")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Time & Recurrence row with Edit & Delete actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = reminder.formattedTime(),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = reminder.formattedSchedule(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (reminder.audioPlaybackMode == "CUSTOM_SINGLE" && !reminder.customAudioTitle.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "🎵 ${reminder.customAudioTitle}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (reminder.audioPlaybackMode == "CUSTOM_RANDOM") {
                        val count = reminder.getAudioCollection().size
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "🔀 Random ($count tracks)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("edit_reminder_${reminder.id}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Reminder", modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_reminder_${reminder.id}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Reminder", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (isSelected) {
            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null
    )
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category.uppercase()) {
        "WAKE_UP" -> Icons.Default.WbSunny
        "BEDTIME" -> Icons.Default.Bedtime
        "WIND_DOWN" -> Icons.Default.Nightlight
        "MEAL" -> Icons.Default.Restaurant
        "MEDICATION" -> Icons.Default.Medication
        "ROUTINE_ANCHOR" -> Icons.Default.Schedule
        "STUDY_BREAK" -> Icons.Default.School
        "HYDRATION" -> Icons.Default.LocalDrink
        else -> Icons.Default.Alarm
    }
}

fun getCategoryLabel(category: String): String {
    return when (category.uppercase()) {
        "WAKE_UP" -> "Wake-Up Anchor"
        "BEDTIME" -> "Bedtime Rest"
        "WIND_DOWN" -> "Bedtime Wind-Down"
        "MEAL" -> "Meal & Nourishment"
        "MEDICATION" -> "Medication Schedule"
        "ROUTINE_ANCHOR" -> "Social Rhythm Anchor"
        "STUDY_BREAK" -> "Study Break"
        "HYDRATION" -> "Hydration Check-in"
        else -> "Custom Reminder"
    }
}

fun getCategoryColor(category: String): Color {
    return when (category.uppercase()) {
        "WAKE_UP" -> Color(0xFFD97706)
        "BEDTIME" -> Color(0xFF4F46E5)
        "WIND_DOWN" -> Color(0xFF7C3AED)
        "MEAL" -> Color(0xFF059669)
        "MEDICATION" -> Color(0xFFE11D48)
        "ROUTINE_ANCHOR" -> Color(0xFF2563EB)
        "STUDY_BREAK" -> Color(0xFF0891B2)
        "HYDRATION" -> Color(0xFF0284C7)
        else -> Color(0xFF4B5563)
    }
}
