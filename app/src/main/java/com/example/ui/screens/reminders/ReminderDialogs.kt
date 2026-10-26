package com.example.ui.screens.reminders

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.ReminderEntity

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditReminderDialog(
    reminder: ReminderEntity?,
    onDismiss: () -> Unit,
    onSave: (ReminderEntity) -> Unit
) {
    val isEditing = reminder != null

    var title by remember { mutableStateOf(reminder?.title ?: "") }
    var category by remember { mutableStateOf(reminder?.category ?: "CUSTOM") }
    var timeMinutes by remember { mutableIntStateOf(reminder?.timeMinutes ?: 480) } // Default 08:00 AM
    var repeatDays by remember { mutableStateOf(reminder?.repeatDays ?: "DAILY") }
    var soundEnabled by remember { mutableStateOf(reminder?.soundEnabled ?: true) }
    var vibrateEnabled by remember { mutableStateOf(reminder?.vibrateEnabled ?: true) }
    var isUrgent by remember { mutableStateOf(reminder?.isUrgent ?: false) }
    var customMessage by remember { mutableStateOf(reminder?.customMessage ?: "") }
    var voiceAnnouncementEnabled by remember { mutableStateOf(reminder?.voiceAnnouncementEnabled ?: false) }
    var spokenAnnouncementText by remember { mutableStateOf(reminder?.spokenAnnouncementText ?: "") }
    var voicePreviewStatus by remember { mutableStateOf<String?>(null) }

    // Feature 4: Custom Audio & Audio Playback Modes
    var audioPlaybackMode by remember { mutableStateOf(reminder?.audioPlaybackMode ?: "DEFAULT") }
    var customAudioUri by remember { mutableStateOf(reminder?.customAudioUri) }
    var customAudioTitle by remember { mutableStateOf(reminder?.customAudioTitle) }
    var customAudioCollectionJson by remember { mutableStateOf(reminder?.customAudioCollectionJson) }
    var audioCollectionList by remember { mutableStateOf(reminder?.getAudioCollection() ?: emptyList()) }
    var previewPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var previewingUri by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    fun stopAudioPreview() {
        try {
            previewPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Exception) {}
        previewPlayer = null
        isPreviewPlaying = false
        previewingUri = null
    }

    fun playAudioPreview(uriStr: String) {
        if (isPreviewPlaying && previewingUri == uriStr) {
            stopAudioPreview()
            return
        }
        stopAudioPreview()
        try {
            val player = android.media.MediaPlayer().apply {
                setDataSource(context, Uri.parse(uriStr))
                setOnCompletionListener { stopAudioPreview() }
                prepare()
                start()
            }
            previewPlayer = player
            isPreviewPlaying = true
            previewingUri = uriStr
        } catch (_: Exception) {
            stopAudioPreview()
        }
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            stopAudioPreview()
        }
    }

    val singleAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            val name = queryAudioDisplayName(context, it) ?: "Custom Audio Track"
            customAudioUri = it.toString()
            customAudioTitle = name
            audioPlaybackMode = "CUSTOM_SINGLE"
        }
    }

    val multiAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            val current = audioCollectionList.toMutableList()
            uris.forEach { u ->
                try {
                    context.contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {}
                val name = queryAudioDisplayName(context, u) ?: "Track ${current.size + 1}"
                if (current.none { it.first == u.toString() }) {
                    current.add(u.toString() to name)
                }
            }
            audioCollectionList = current
            val jsonArr = org.json.JSONArray()
            current.forEach { (u, t) ->
                val obj = org.json.JSONObject()
                obj.put("uri", u)
                obj.put("title", t)
                jsonArr.put(obj)
            }
            customAudioCollectionJson = jsonArr.toString()
            audioPlaybackMode = "CUSTOM_RANDOM"
        }
    }

    var expandedCategoryMenu by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    val categories = listOf(
        "WAKE_UP" to "Wake-Up",
        "BEDTIME" to "Bedtime",
        "WIND_DOWN" to "Bedtime Wind-Down",
        "MEAL" to "Meal / Nourishment",
        "MEDICATION" to "Medication",
        "ROUTINE_ANCHOR" to "Routine Anchor",
        "STUDY_BREAK" to "Study Break",
        "HYDRATION" to "Hydration",
        "CUSTOM" to "Custom Reminder"
    )

    val weekDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    val isDailySelected = repeatDays.equals("DAILY", ignoreCase = true) || repeatDays.isBlank()
    val isWeekdaysSelected = repeatDays.equals("WEEKDAYS", ignoreCase = true)
    val isWeekendsSelected = repeatDays.equals("WEEKENDS", ignoreCase = true)
    val isCustomDaysSelected = !isDailySelected && !isWeekdaysSelected && !isWeekendsSelected
    val activeDaysSet = when {
        isDailySelected -> setOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
        isWeekdaysSelected -> setOf("MON", "TUE", "WED", "THU", "FRI")
        isWeekendsSelected -> setOf("SAT", "SUN")
        else -> repeatDays.split(",").map { it.trim().uppercase() }.toSet()
    }

    AlertDialog(
        onDismissRequest = {
            stopAudioPreview()
            onDismiss()
        },
        title = {
            Text(
                text = if (isEditing) "Edit Reminder" else "Add New Reminder",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Reminder Title *") },
                    placeholder = { Text("e.g. Afternoon Water, Multivitamin") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_dialog_title_input")
                )

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedCategoryMenu,
                    onExpandedChange = { expandedCategoryMenu = !expandedCategoryMenu },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = categories.find { it.first == category }?.second ?: category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoryMenu) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .testTag("reminder_dialog_category_dropdown")
                    )

                    ExposedDropdownMenu(
                        expanded = expandedCategoryMenu,
                        onDismissRequest = { expandedCategoryMenu = false }
                    ) {
                        categories.forEach { (catKey, catLabel) ->
                            DropdownMenuItem(
                                text = { Text(catLabel) },
                                onClick = {
                                    category = catKey
                                    expandedCategoryMenu = false
                                }
                            )
                        }
                    }
                }

                // Time Selection Button / Card
                Card(
                    onClick = { showTimePickerDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_dialog_time_picker_trigger")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("Scheduled Time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    formatMinutesToAmPm(timeMinutes),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        TextButton(onClick = { showTimePickerDialog = true }) {
                            Text("Change")
                        }
                    }
                }

                // Repeat Schedule (Feature 4 B: Every day, Weekdays, Weekends, Custom)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Repeat Schedule",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Presets
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = isDailySelected,
                            onClick = { repeatDays = "DAILY" },
                            label = { Text("Every day") },
                            leadingIcon = if (isDailySelected) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null,
                            modifier = Modifier.testTag("chip_daily")
                        )

                        FilterChip(
                            selected = isWeekdaysSelected,
                            onClick = { repeatDays = "WEEKDAYS" },
                            label = { Text("Weekdays (Mon–Fri)") },
                            leadingIcon = if (isWeekdaysSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null,
                            modifier = Modifier.testTag("chip_weekdays")
                        )

                        FilterChip(
                            selected = isWeekendsSelected,
                            onClick = { repeatDays = "WEEKENDS" },
                            label = { Text("Weekends (Sat–Sun)") },
                            leadingIcon = if (isWeekendsSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null,
                            modifier = Modifier.testTag("chip_weekends")
                        )

                        FilterChip(
                            selected = isCustomDaysSelected,
                            onClick = {
                                if (!isCustomDaysSelected) repeatDays = "MON,WED,FRI"
                            },
                            label = { Text("Custom selection") },
                            leadingIcon = if (isCustomDaysSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null,
                            modifier = Modifier.testTag("chip_custom_days")
                        )
                    }

                    // Individual day toggles
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        weekDays.forEach { day ->
                            val isSelected = activeDaysSet.contains(day)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val currentSet = activeDaysSet.toMutableSet()
                                    if (isSelected) {
                                        currentSet.remove(day)
                                    } else {
                                        currentSet.add(day)
                                    }
                                    repeatDays = when {
                                        currentSet.isEmpty() || currentSet.size == 7 -> "DAILY"
                                        currentSet == setOf("MON", "TUE", "WED", "THU", "FRI") -> "WEEKDAYS"
                                        currentSet == setOf("SAT", "SUN") -> "WEEKENDS"
                                        else -> weekDays.filter { currentSet.contains(it) }.joinToString(",")
                                    }
                                },
                                label = { Text(day, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // Sound & Vibration Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Sound Alert", style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { soundEnabled = it },
                        modifier = Modifier.testTag("reminder_dialog_sound_switch")
                    )
                }

                // Feature 4: Custom Audio & Audio Playback Mode Card
                if (soundEnabled) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("reminder_dialog_audio_section")
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text("Alarm Audio Sound", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                    Text("Select default ringtone, single audio file, or random playlist", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            // Mode selection
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = audioPlaybackMode == "DEFAULT",
                                    onClick = { audioPlaybackMode = "DEFAULT" },
                                    label = { Text("Default Sound") }
                                )
                                FilterChip(
                                    selected = audioPlaybackMode == "CUSTOM_SINGLE",
                                    onClick = { audioPlaybackMode = "CUSTOM_SINGLE" },
                                    label = { Text("Single Track") }
                                )
                                FilterChip(
                                    selected = audioPlaybackMode == "CUSTOM_RANDOM",
                                    onClick = { audioPlaybackMode = "CUSTOM_RANDOM" },
                                    label = { Text("Random Collection") }
                                )
                            }

                            when (audioPlaybackMode) {
                                "DEFAULT" -> {
                                    Text(
                                        "Plays Android's system default alarm chime or gentle notification tone.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                "CUSTOM_SINGLE" -> {
                                    if (!customAudioUri.isNullOrBlank()) {
                                        val isAccessible = isUriAccessible(context, customAudioUri)
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "🎵 ${customAudioTitle ?: "Custom Audio Track"}",
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                        maxLines = 1
                                                    )
                                                    if (!isAccessible) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                                            Text(
                                                                "File missing or inaccessible",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.error
                                                            )
                                                        }
                                                    }
                                                }

                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    IconButton(
                                                        onClick = { playAudioPreview(customAudioUri!!) },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isPreviewPlaying && previewingUri == customAudioUri) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                            contentDescription = "Preview Audio"
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            stopAudioPreview()
                                                            customAudioUri = null
                                                            customAudioTitle = null
                                                            audioPlaybackMode = "DEFAULT"
                                                        },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(Icons.Default.Close, contentDescription = "Remove Audio", tint = MaterialTheme.colorScheme.error)
                                                    }
                                                }
                                            }

                                            OutlinedButton(
                                                onClick = { singleAudioLauncher.launch(arrayOf("audio/*")) },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Choose Replacement File")
                                            }
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { singleAudioLauncher.launch(arrayOf("audio/*")) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Choose Local Audio File")
                                        }
                                    }
                                }
                                "CUSTOM_RANDOM" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            "Randomly plays a track from this collection when the alarm triggers, avoiding consecutive repeats.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (audioCollectionList.isNotEmpty()) {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                audioCollectionList.forEachIndexed { index, (uri, name) ->
                                                    val accessible = isUriAccessible(context, uri)
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                                            Column {
                                                                Text(name, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                                                if (!accessible) {
                                                                    Text("Inaccessible", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                                                }
                                                            }
                                                        }

                                                        Row {
                                                            IconButton(
                                                                onClick = { playAudioPreview(uri) },
                                                                modifier = Modifier.size(32.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = if (isPreviewPlaying && previewingUri == uri) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                                    contentDescription = "Preview",
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                            IconButton(
                                                                onClick = {
                                                                    stopAudioPreview()
                                                                    val updatedList = audioCollectionList.toMutableList().also { it.removeAt(index) }
                                                                    audioCollectionList = updatedList
                                                                    val arr = org.json.JSONArray()
                                                                    updatedList.forEach { (u, t) ->
                                                                        arr.put(org.json.JSONObject().put("uri", u).put("title", t))
                                                                    }
                                                                    customAudioCollectionJson = if (updatedList.isEmpty()) null else arr.toString()
                                                                },
                                                                modifier = Modifier.size(32.dp)
                                                            ) {
                                                                Icon(Icons.Default.Close, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = { multiAudioLauncher.launch(arrayOf("audio/*")) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Add Tracks to Collection")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Vibration, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Vibration", style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(
                        checked = vibrateEnabled,
                        onCheckedChange = { vibrateEnabled = it },
                        modifier = Modifier.testTag("reminder_dialog_vibrate_switch")
                    )
                }

                // Critical / Urgent Toggle (Quiet hours bypass)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUrgent) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PriorityHigh, contentDescription = null, tint = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                            Column {
                                Text(
                                    "Critical / Urgent Alert",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    "Bypasses Quiet Hours (ideal for vital medications)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isUrgent,
                            onCheckedChange = { isUrgent = it },
                            modifier = Modifier.testTag("reminder_dialog_urgent_switch")
                        )
                    }
                }

                // Custom Message / Note
                OutlinedTextField(
                    value = customMessage,
                    onValueChange = { customMessage = it },
                    label = { Text("Hint / Subtitle (Optional)") },
                    placeholder = { Text("e.g. Take with a glass of water") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Optional Voice Announcement (TTS)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (voiceAnnouncementEnabled) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("reminder_dialog_voice_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = if (voiceAnnouncementEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Column {
                                    Text(
                                        "Spoken Voice Announcement",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        "Speaks aloud via TTS (Disabled by default)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = voiceAnnouncementEnabled,
                                onCheckedChange = {
                                    voiceAnnouncementEnabled = it
                                    if (!it) {
                                        com.example.util.VoiceAnnouncementHelper.stopPreview()
                                        voicePreviewStatus = null
                                    }
                                },
                                modifier = Modifier.testTag("reminder_dialog_voice_switch")
                            )
                        }

                        if (voiceAnnouncementEnabled) {
                            OutlinedTextField(
                                value = spokenAnnouncementText,
                                onValueChange = { spokenAnnouncementText = it },
                                label = { Text("Spoken Speech Text") },
                                placeholder = {
                                    Text("Default: It's time for your scheduled reminder: ${title.ifBlank { "Reminder" }}")
                                },
                                minLines = 2,
                                maxLines = 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reminder_dialog_spoken_text_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        val textToSpeak = spokenAnnouncementText.trim().ifBlank {
                                            "It's time for your scheduled reminder: ${title.ifBlank { "reminder" }}."
                                        }
                                        com.example.util.VoiceAnnouncementHelper.preview(context, textToSpeak) { status ->
                                            voicePreviewStatus = status
                                        }
                                    },
                                    modifier = Modifier.testTag("reminder_dialog_preview_voice_btn")
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

                            Text(
                                text = "Volume is controlled through Android system audio settings. Silenced during quiet hours unless marked as Urgent.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (category == "MEDICATION") {
                            Text(
                                text = "Medical note: Reminders and announcements provide scheduled alerts, not proof that medication was taken.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        stopAudioPreview()
                        com.example.util.VoiceAnnouncementHelper.stopPreview()
                        val updated = ReminderEntity(
                            id = reminder?.id ?: 0L,
                            title = title.trim(),
                            category = category,
                            timeMinutes = timeMinutes,
                            repeatDays = repeatDays,
                            isEnabled = reminder?.isEnabled ?: true,
                            soundEnabled = soundEnabled,
                            vibrateEnabled = vibrateEnabled,
                            isUrgent = isUrgent,
                            voiceAnnouncementEnabled = voiceAnnouncementEnabled,
                            spokenAnnouncementText = spokenAnnouncementText.trim().ifBlank { null },
                            customAudioUri = customAudioUri,
                            customAudioTitle = customAudioTitle,
                            customAudioCollectionJson = customAudioCollectionJson,
                            audioPlaybackMode = audioPlaybackMode,
                            lastPlayedAudioUri = reminder?.lastPlayedAudioUri,
                            referenceId = reminder?.referenceId,
                            customMessage = customMessage.trim().ifBlank { null },
                            createdAt = reminder?.createdAt ?: System.currentTimeMillis()
                        )
                        onSave(updated)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("reminder_dialog_save_button")
            ) {
                Text(if (isEditing) "Save Changes" else "Add Reminder")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    stopAudioPreview()
                    com.example.util.VoiceAnnouncementHelper.stopPreview()
                    onDismiss()
                },
                modifier = Modifier.testTag("reminder_dialog_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )

    if (showTimePickerDialog) {
        TimeSelectionDialog(
            initialMinutes = timeMinutes,
            onDismiss = { showTimePickerDialog = false },
            onConfirm = { selected ->
                timeMinutes = selected
                showTimePickerDialog = false
            }
        )
    }
}

@Composable
fun TimeSelectionDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var hours24 = initialMinutes / 60
    var minutes = initialMinutes % 60

    var isPm by remember { mutableStateOf(hours24 >= 12) }
    var selectedHour12 by remember {
        val h = if (hours24 % 12 == 0) 12 else hours24 % 12
        mutableIntStateOf(h)
    }
    var selectedMinute by remember { mutableIntStateOf(minutes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select Time", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Big AM / PM selector and Display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = String.format(java.util.Locale.US, "%02d:%02d", selectedHour12, selectedMinute),
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = !isPm,
                            onClick = { isPm = false },
                            label = { Text("AM") },
                            modifier = Modifier.testTag("time_chip_am")
                        )
                        FilterChip(
                            selected = isPm,
                            onClick = { isPm = true },
                            label = { Text("PM") },
                            modifier = Modifier.testTag("time_chip_pm")
                        )
                    }
                }

                // Hour adjustment
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Hour:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                selectedHour12 = if (selectedHour12 <= 1) 12 else selectedHour12 - 1
                            }
                        ) {
                            Text("-")
                        }
                        Text(
                            "$selectedHour12",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Button(
                            onClick = {
                                selectedHour12 = if (selectedHour12 >= 12) 1 else selectedHour12 + 1
                            }
                        ) {
                            Text("+")
                        }
                    }
                }

                // Minute adjustment (steps of 5)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Minute:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                selectedMinute = (selectedMinute - 5 + 60) % 60
                            }
                        ) {
                            Text("-5")
                        }
                        Text(
                            String.format(java.util.Locale.US, "%02d", selectedMinute),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Button(
                            onClick = {
                                selectedMinute = (selectedMinute + 5) % 60
                            }
                        ) {
                            Text("+5")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val computedHour24 = when {
                        isPm && selectedHour12 < 12 -> selectedHour12 + 12
                        !isPm && selectedHour12 == 12 -> 0
                        else -> selectedHour12
                    }
                    onConfirm(computedHour24 * 60 + selectedMinute)
                },
                modifier = Modifier.testTag("time_dialog_confirm_button")
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun QuietHoursScheduleDialog(
    initialStartMinutes: Int,
    initialEndMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (startMin: Int, endMin: Int) -> Unit
) {
    var startMinutes by remember { mutableIntStateOf(initialStartMinutes) }
    var endMinutes by remember { mutableIntStateOf(initialEndMinutes) }
    var editingWhich by remember { mutableStateOf<String?>(null) } // "start" or "end"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Quiet Hours Schedule", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "During quiet hours, routine and study reminders remain silent or suppressed. Urgent medications bypass this window.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    onClick = { editingWhich = "start" },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().testTag("quiet_start_btn")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Starts (Wind-Down)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatMinutesToAmPm(startMinutes), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        TextButton(onClick = { editingWhich = "start" }) { Text("Change") }
                    }
                }

                Card(
                    onClick = { editingWhich = "end" },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().testTag("quiet_end_btn")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Ends (Wake-Up)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatMinutesToAmPm(endMinutes), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        TextButton(onClick = { editingWhich = "end" }) { Text("Change") }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(startMinutes, endMinutes) },
                modifier = Modifier.testTag("save_quiet_hours_button")
            ) {
                Text("Save Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (editingWhich == "start") {
        TimeSelectionDialog(
            initialMinutes = startMinutes,
            onDismiss = { editingWhich = null },
            onConfirm = {
                startMinutes = it
                editingWhich = null
            }
        )
    } else if (editingWhich == "end") {
        TimeSelectionDialog(
            initialMinutes = endMinutes,
            onDismiss = { editingWhich = null },
            onConfirm = {
                endMinutes = it
                editingWhich = null
            }
        )
    }
}

fun formatMinutesToAmPm(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    val isPm = hours >= 12
    val displayHour = when {
        hours == 0 -> 12
        hours > 12 -> hours - 12
        else -> hours
    }
    val amPm = if (isPm) "PM" else "AM"
    return String.format(java.util.Locale.US, "%d:%02d %s", displayHour, mins, amPm)
}

fun queryAudioDisplayName(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                cursor.getString(nameIndex)
            } else null
        } ?: uri.lastPathSegment
    } catch (_: Exception) {
        uri.lastPathSegment
    }
}

fun isUriAccessible(context: Context, uriString: String?): Boolean {
    if (uriString.isNullOrBlank()) return false
    return try {
        context.contentResolver.openFileDescriptor(Uri.parse(uriString), "r")?.use { true } ?: false
    } catch (_: Exception) {
        false
    }
}
