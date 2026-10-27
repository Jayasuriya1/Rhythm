package com.example.ui.screens.alarm

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CustomAlarmEntity

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlarmEditDialog(
    alarm: CustomAlarmEntity?,
    onDismiss: () -> Unit,
    onSave: (CustomAlarmEntity) -> Unit
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf(alarm?.title ?: "Morning Rise") }
    var hour by remember { mutableIntStateOf((alarm?.timeMinutes ?: 420) / 60) }
    var minute by remember { mutableIntStateOf((alarm?.timeMinutes ?: 420) % 60) }
    var isPm by remember { mutableStateOf(hour >= 12) }

    var repeatScheduleType by remember {
        mutableStateOf(
            when {
                alarm == null -> "WEEKDAYS"
                alarm.repeatDays.equals("DAILY", true) -> "DAILY"
                alarm.repeatDays.equals("WEEKDAYS", true) -> "WEEKDAYS"
                alarm.repeatDays.equals("WEEKENDS", true) -> "WEEKENDS"
                else -> "CUSTOM"
            }
        )
    }

    val allDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    var selectedDays by remember {
        mutableStateOf(
            if (alarm != null && !alarm.repeatDays.equals("DAILY", true) &&
                !alarm.repeatDays.equals("WEEKDAYS", true) &&
                !alarm.repeatDays.equals("WEEKENDS", true)
            ) {
                alarm.repeatDays.split(",").map { it.trim().uppercase() }.toSet()
            } else {
                setOf("MON", "TUE", "WED", "THU", "FRI")
            }
        )
    }

    // Audio states
    var customAudioUri by remember { mutableStateOf(alarm?.customAudioUri) }
    var customAudioTitle by remember { mutableStateOf(alarm?.customAudioTitle ?: "Gentle Sunrise Chime") }
    var crescendoSeconds by remember { mutableIntStateOf(alarm?.crescendoDurationSeconds ?: 60) }
    var isLooping by remember { mutableStateOf(alarm?.isLooping ?: true) }
    var vibrateEnabled by remember { mutableStateOf(alarm?.vibrateEnabled ?: true) }

    // Visual Wallpaper states
    var wallpaperUri by remember { mutableStateOf(alarm?.wallpaperUri) }
    var wallpaperPresetColor by remember { mutableStateOf(alarm?.wallpaperPresetColor ?: "MIDNIGHT_INDIGO") }
    var timeClockFont by remember { mutableStateOf(alarm?.timeClockFont ?: "MODERN_DIGITAL") }

    // Mission states
    var missionType by remember { mutableStateOf(alarm?.missionType ?: "NONE") }
    var targetBarcode by remember { mutableStateOf(alarm?.targetBarcode ?: "") }
    var targetBarcodeLabel by remember { mutableStateOf(alarm?.targetBarcodeLabel ?: "") }
    var chainedBackupAlarmEnabled by remember { mutableStateOf(alarm?.chainedBackupAlarmEnabled ?: true) }

    // Audio Preview Player
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                previewPlayer?.stop()
                previewPlayer?.release()
            } catch (_: Exception) {}
        }
    }

    fun stopPreview() {
        try {
            previewPlayer?.stop()
            previewPlayer?.release()
        } catch (_: Exception) {}
        previewPlayer = null
        isPreviewPlaying = false
    }

    fun startPreview(uriStr: String?) {
        stopPreview()
        if (uriStr.isNullOrBlank()) return
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context, Uri.parse(uriStr))
                prepare()
                start()
                setOnCompletionListener {
                    isPreviewPlaying = false
                }
            }
            previewPlayer = player
            isPreviewPlaying = true
        } catch (_: Exception) {
            isPreviewPlaying = false
        }
    }

    // Audio Document Picker with persistable permissions
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            var filename = "Custom Audio Track"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        filename = cursor.getString(nameIndex) ?: filename
                    }
                }
            }
            customAudioUri = uri.toString()
            customAudioTitle = filename
        }
    }

    // Wallpaper Photo Picker
    val wallpaperPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            wallpaperUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = {
            stopPreview()
            onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = "Alarm",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (alarm == null) "New Custom Alarm" else "Edit Alarm")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Alarm Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Time Selection
                Text(
                    text = "Alarm Time",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                val displayHour12 = when {
                    hour == 0 -> 12
                    hour in 1..12 -> hour
                    else -> hour - 12
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour +/-
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = {
                            var newH = (displayHour12 % 12) + 1
                            if (isPm && newH != 12) newH += 12
                            if (!isPm && newH == 12) newH = 0
                            hour = newH
                        }) { Text("▲", fontWeight = FontWeight.Bold) }
                        Text(
                            text = String.format(java.util.Locale.US, "%02d", displayHour12),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = {
                            var newH = displayHour12 - 1
                            if (newH < 1) newH = 12
                            if (isPm && newH != 12) newH += 12
                            if (!isPm && newH == 12) newH = 0
                            hour = newH
                        }) { Text("▼", fontWeight = FontWeight.Bold) }
                    }

                    Text(":", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))

                    // Minute +/-
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { minute = (minute + 5) % 60 }) { Text("▲", fontWeight = FontWeight.Bold) }
                        Text(
                            text = String.format(java.util.Locale.US, "%02d", minute),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { minute = (minute - 5 + 60) % 60 }) { Text("▼", fontWeight = FontWeight.Bold) }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // AM / PM Toggle
                    Column {
                        FilterChip(
                            selected = !isPm,
                            onClick = {
                                if (isPm) {
                                    isPm = false
                                    hour = if (hour == 12) 0 else hour - 12
                                }
                            },
                            label = { Text("AM") }
                        )
                        FilterChip(
                            selected = isPm,
                            onClick = {
                                if (!isPm) {
                                    isPm = true
                                    hour = if (hour == 0) 12 else hour + 12
                                }
                            },
                            label = { Text("PM") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Weekday Schedule
                Text(
                    text = "Schedule Days",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("WEEKDAYS", "WEEKENDS", "DAILY", "CUSTOM").forEach { type ->
                        FilterChip(
                            selected = repeatScheduleType == type,
                            onClick = { repeatScheduleType = type },
                            label = {
                                Text(
                                    when (type) {
                                        "WEEKDAYS" -> "Weekdays"
                                        "WEEKENDS" -> "Weekends"
                                        "DAILY" -> "Every day"
                                        else -> "Custom"
                                    }
                                )
                            }
                        )
                    }
                }

                if (repeatScheduleType == "CUSTOM") {
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        allDays.forEach { day ->
                            FilterChip(
                                selected = selectedDays.contains(day),
                                onClick = {
                                    selectedDays = if (selectedDays.contains(day)) {
                                        if (selectedDays.size > 1) selectedDays - day else selectedDays
                                    } else {
                                        selectedDays + day
                                    }
                                },
                                label = { Text(day) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Audio Architecture Section
                Text(
                    text = "Audio & Crescendo",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Audiotrack, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = customAudioTitle,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (customAudioUri != null) "Custom local track" else "Built-in soothing tone",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!customAudioUri.isNullOrBlank()) {
                                IconButton(onClick = {
                                    if (isPreviewPlaying) stopPreview() else startPreview(customAudioUri)
                                }) {
                                    Icon(
                                        imageVector = if (isPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = "Preview"
                                    )
                                }
                            }

                            OutlinedButton(onClick = {
                                audioPickerLauncher.launch(arrayOf("audio/*"))
                            }) {
                                Text("Pick File", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Crescendo Window Selector
                        Text("Volume Fade-In (Crescendo)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                0 to "Instant",
                                30 to "30 sec",
                                60 to "60 sec",
                                120 to "2 min"
                            ).forEach { (sec, label) ->
                                FilterChip(
                                    selected = crescendoSeconds == sec,
                                    onClick = { crescendoSeconds = sec },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Continuous Audio Loop", fontSize = 13.sp)
                            Switch(checked = isLooping, onCheckedChange = { isLooping = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Vibrate", fontSize = 13.sp)
                            Switch(checked = vibrateEnabled, onCheckedChange = { vibrateEnabled = it })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Visual Customisation Section
                Text(
                    text = "Visuals & Lockscreen Wallpaper",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (!wallpaperUri.isNullOrBlank()) "Custom Photo Wallpaper" else "Preset Color Gradient",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (!wallpaperUri.isNullOrBlank()) "Uploaded from gallery" else wallpaperPresetColor.replace("_", " "),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!wallpaperUri.isNullOrBlank()) {
                                TextButton(onClick = { wallpaperUri = null }) {
                                    Text("Reset", color = MaterialTheme.colorScheme.error)
                                }
                            }

                            OutlinedButton(onClick = {
                                wallpaperPickerLauncher.launch(arrayOf("image/*"))
                            }) {
                                Text("Choose Image", fontSize = 12.sp)
                            }
                        }

                        if (wallpaperUri.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Calming Color Presets", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(6.dp))

                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    "MIDNIGHT_INDIGO" to Color(0xFF1E293B),
                                    "SUNRISE_GOLD" to Color(0xFFE65100),
                                    "FOREST_EMERALD" to Color(0xFF1B4332),
                                    "DEEP_CRIMSON" to Color(0xFF590D22),
                                    "COSMIC_VIOLET" to Color(0xFF240046),
                                    "OBSIDIAN" to Color(0xFF1E1E1E)
                                ).forEach { (id, col) ->
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(col)
                                            .border(
                                                width = if (wallpaperPresetColor == id) 3.dp else 1.dp,
                                                color = if (wallpaperPresetColor == id) MaterialTheme.colorScheme.primary else Color.Gray,
                                                shape = CircleShape
                                            )
                                            .clickable { wallpaperPresetColor = id }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Clock Font Selector
                        Text("Clock Display Font", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                "MODERN_DIGITAL" to "Modern Digital",
                                "MONOSPACE" to "Monospace",
                                "SERIF_ELEGANT" to "Serif Elegant",
                                "NEO_GROTESQUE" to "Neo-Grotesque"
                            ).forEach { (id, label) ->
                                FilterChip(
                                    selected = timeClockFont == id,
                                    onClick = { timeClockFont = id },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Behavioral Failsafes & Accessibility Controls
                Text(
                    text = "Behavioral Failsafes & Missions",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Dismissal Mission", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = missionType == "NONE",
                                onClick = { missionType = "NONE" },
                                label = { Text("Swipe to Dismiss") }
                            )
                            FilterChip(
                                selected = missionType == "BARCODE",
                                onClick = { missionType = "BARCODE" },
                                label = { Text("QR / Barcode CAPTCHA") }
                            )
                        }

                        if (missionType == "BARCODE") {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = targetBarcode,
                                onValueChange = { targetBarcode = it },
                                label = { Text("Target Barcode / QR Code Value") },
                                placeholder = { Text("e.g. 012345678905") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = targetBarcodeLabel,
                                onValueChange = { targetBarcodeLabel = it },
                                label = { Text("Item Name / Location Hint") },
                                placeholder = { Text("e.g. Bathroom Toothpaste") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Scan any household item barcode to ensure you get out of bed to dismiss the alarm.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Chained Backup Alarm Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Chained Backup Alarm", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    "Triggers aggressive alarm 5 minutes later if mission uncompleted",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = chainedBackupAlarmEnabled,
                                onCheckedChange = { chainedBackupAlarmEnabled = it }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    stopPreview()
                    val calculatedTimeMinutes = hour * 60 + minute
                    val computedRepeatDays = when (repeatScheduleType) {
                        "WEEKDAYS" -> "WEEKDAYS"
                        "WEEKENDS" -> "WEEKENDS"
                        "DAILY" -> "DAILY"
                        else -> selectedDays.joinToString(",")
                    }

                    val savedEntity = CustomAlarmEntity(
                        id = alarm?.id ?: 0L,
                        title = title.ifBlank { "Morning Wake-Up" },
                        timeMinutes = calculatedTimeMinutes,
                        repeatDays = computedRepeatDays,
                        isEnabled = alarm?.isEnabled ?: true,
                        vibrateEnabled = vibrateEnabled,
                        customAudioUri = customAudioUri,
                        customAudioTitle = customAudioTitle,
                        crescendoDurationSeconds = crescendoSeconds,
                        isLooping = isLooping,
                        wallpaperUri = wallpaperUri,
                        wallpaperPresetColor = wallpaperPresetColor,
                        timeClockFont = timeClockFont,
                        missionType = missionType,
                        targetBarcode = if (missionType == "BARCODE") targetBarcode.trim() else null,
                        targetBarcodeLabel = if (missionType == "BARCODE") targetBarcodeLabel.trim() else null,
                        chainedBackupAlarmEnabled = chainedBackupAlarmEnabled,
                        createdAt = alarm?.createdAt ?: System.currentTimeMillis()
                    )
                    onSave(savedEntity)
                }
            ) {
                Text("Save Alarm")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                stopPreview()
                onDismiss()
            }) {
                Text("Cancel")
            }
        }
    )
}
