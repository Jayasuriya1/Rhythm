package com.example.ui.screens.alarm

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.CustomAlarmEntity
import com.example.data.entity.RegisteredCodeEntity

/**
 * Modern, visually rich Alarm Studio & Editor.
 *
 * Features:
 * - Live interactive lockscreen phone preview showing the active wallpaper, time in chosen typography,
 *   alarm label, and dismissal mission.
 * - Tabbed segmented navigation (Time & Days, Background & Font, Audio & Missions).
 * - Visual gradient wallpaper selector cards and custom photo gallery picker.
 * - Touch-friendly big time selector with quick shortcut buttons.
 * - Crescendo volume preview and QR/Barcode mission verification.
 */
@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditDialog(
    alarm: CustomAlarmEntity?,
    registeredCodes: List<RegisteredCodeEntity> = emptyList(),
    onSaveNewRegisteredCode: ((RegisteredCodeEntity) -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (CustomAlarmEntity) -> Unit
) {
    val context = LocalContext.current
    val isCompactHeight = LocalConfiguration.current.screenHeightDp < 720

    var activeTab by remember { mutableIntStateOf(0) }

    var title by remember { mutableStateOf(alarm?.title ?: "Morning Rise") }
    var hour by remember { mutableIntStateOf((alarm?.timeMinutes ?: 420) / 60) }
    var minute by remember { mutableIntStateOf((alarm?.timeMinutes ?: 420) % 60) }
    var isPm by remember { mutableStateOf(hour >= 12) }

    var repeatScheduleType by remember {
        mutableStateOf(
            when {
                alarm == null -> "WEEKDAYS"
                alarm.repeatDays.equals("ONCE", true) -> "ONCE"
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
                !alarm.repeatDays.equals("WEEKENDS", true) &&
                !alarm.repeatDays.equals("ONCE", true)
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
    var targetBarcodeFormat by remember { mutableStateOf(alarm?.targetBarcodeFormat ?: "QR_CODE") }
    var targetBarcodeLocation by remember { mutableStateOf(alarm?.targetBarcodeLocation ?: "") }
    var registeredCodeId by remember { mutableStateOf(alarm?.registeredCodeId) }
    var chainedBackupAlarmEnabled by remember { mutableStateOf(alarm?.chainedBackupAlarmEnabled ?: true) }

    var showHouseholdScanner by remember { mutableStateOf(false) }
    var showCodesPickerSheet by remember { mutableStateOf(false) }
    var showTestScan by remember { mutableStateOf(false) }
    var testSuccessMessage by remember { mutableStateOf<String?>(null) }

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
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                if (!uriStr.isNullOrBlank()) {
                    setDataSource(context, Uri.parse(uriStr))
                } else {
                    val defaultUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                    setDataSource(context, defaultUri)
                }
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

    // Audio Document Picker
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

    // Custom Bitmap Resolver for live preview
    val customWallpaperBitmap = remember(wallpaperUri) {
        if (!wallpaperUri.isNullOrBlank()) {
            try {
                val uri = Uri.parse(wallpaperUri)
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                }
            } catch (_: Exception) {
                null
            }
        } else null
    }

    val displayHour12 = when {
        hour == 0 -> 12
        hour in 1..12 -> hour
        else -> hour - 12
    }
    val amPmString = if (isPm) "PM" else "AM"
    val timeFormattedString = String.format(java.util.Locale.US, "%02d:%02d %s", displayHour12, minute, amPmString)

    Dialog(
        onDismissRequest = {
            stopPreview()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .widthIn(max = 580.dp)
                .fillMaxHeight(0.94f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Modern Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = if (isCompactHeight) 8.dp else 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (alarm == null) "New Alarm" else "Edit Alarm",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Precision Wake-Up & Wallpaper Studio",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            stopPreview()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("close_alarm_edit_dialog_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // 1. LIVE PHONE SCREEN PREVIEW (Pinned Visual Highlight)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    AlarmLiveScreenPreviewMockup(
                        title = title,
                        timeFormatted = timeFormattedString,
                        fontType = timeClockFont,
                        presetColor = wallpaperPresetColor,
                        customBitmap = customWallpaperBitmap,
                        missionType = missionType,
                        missionLabel = targetBarcodeLabel,
                        audioTitle = customAudioTitle,
                        crescendoSeconds = crescendoSeconds,
                        isCompactHeight = isCompactHeight
                    )
                }

                // Modern Primary Tab Row
                PrimaryTabRow(
                    selectedTabIndex = activeTab,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Time & Days", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Wallpaper & Style", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.ColorLens, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = { Text("Audio & Mission", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                // Tab Content Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    when (activeTab) {
                        0 -> TimeAndScheduleTabContent(
                            title = title,
                            onTitleChange = { title = it },
                            displayHour12 = displayHour12,
                            minute = minute,
                            isPm = isPm,
                            onHourChange = { newHour12 ->
                                var newH = if (newHour12 == 12) 0 else newHour12
                                if (isPm) newH += 12
                                hour = newH
                            },
                            onMinuteChange = { minute = it.coerceIn(0, 59) },
                            onToggleAmPm = { pm ->
                                isPm = pm
                                var newH = displayHour12 % 12
                                if (pm) newH += 12
                                hour = newH
                            },
                            repeatScheduleType = repeatScheduleType,
                            onRepeatScheduleTypeChange = { repeatScheduleType = it },
                            selectedDays = selectedDays,
                            onToggleDay = { day ->
                                selectedDays = if (selectedDays.contains(day)) {
                                    if (selectedDays.size > 1) selectedDays - day else selectedDays
                                } else {
                                    selectedDays + day
                                }
                            }
                        )

                        1 -> WallpaperAndStyleTabContent(
                            wallpaperUri = wallpaperUri,
                            wallpaperPresetColor = wallpaperPresetColor,
                            timeClockFont = timeClockFont,
                            onSelectPresetColor = {
                                wallpaperUri = null
                                wallpaperPresetColor = it
                            },
                            onPickCustomImage = {
                                wallpaperPickerLauncher.launch(arrayOf("image/*"))
                            },
                            onResetToPreset = {
                                wallpaperUri = null
                            },
                            onSelectClockFont = { timeClockFont = it }
                        )

                        2 -> AudioAndMissionTabContent(
                            customAudioTitle = customAudioTitle,
                            customAudioUri = customAudioUri,
                            isPreviewPlaying = isPreviewPlaying,
                            onTogglePreview = {
                                if (isPreviewPlaying) stopPreview() else startPreview(customAudioUri)
                            },
                            onPickAudioFile = {
                                audioPickerLauncher.launch(arrayOf("audio/*"))
                            },
                            crescendoSeconds = crescendoSeconds,
                            onCrescendoChange = { crescendoSeconds = it },
                            isLooping = isLooping,
                            onLoopChange = { isLooping = it },
                            vibrateEnabled = vibrateEnabled,
                            onVibrateChange = { vibrateEnabled = it },
                            missionType = missionType,
                            onMissionTypeChange = { missionType = it },
                            targetBarcode = targetBarcode,
                            targetBarcodeLabel = targetBarcodeLabel,
                            targetBarcodeFormat = targetBarcodeFormat,
                            targetBarcodeLocation = targetBarcodeLocation,
                            onScanHouseholdItem = { showHouseholdScanner = true },
                            onPickFromLibrary = { showCodesPickerSheet = true },
                            onTestScan = { showTestScan = true },
                            onChangeBarcode = {
                                targetBarcode = ""
                                targetBarcodeLabel = ""
                                registeredCodeId = null
                            },
                            onManualBarcodeChange = { code, lbl ->
                                targetBarcode = code
                                targetBarcodeLabel = lbl
                            },
                            testSuccessMessage = testSuccessMessage,
                            registeredCodesCount = registeredCodes.size,
                            chainedBackupAlarmEnabled = chainedBackupAlarmEnabled,
                            onChainedBackupChange = { chainedBackupAlarmEnabled = it }
                        )
                    }
                }

                // Bottom Action Footer
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                stopPreview()
                                onDismiss()
                            },
                            modifier = Modifier.testTag("cancel_alarm_button")
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                stopPreview()
                                val calculatedTimeMinutes = hour * 60 + minute
                                val computedRepeatDays = when (repeatScheduleType) {
                                    "ONCE" -> "ONCE"
                                    "WEEKDAYS" -> "WEEKDAYS"
                                    "WEEKENDS" -> "WEEKENDS"
                                    "DAILY" -> "DAILY"
                                    else -> selectedDays.joinToString(",")
                                }

                                val savedEntity = CustomAlarmEntity(
                                    id = alarm?.id ?: 0L,
                                    title = title.ifBlank { "Morning Rise" },
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
                                    targetBarcode = if (missionType == "BARCODE") targetBarcode.trim().ifBlank { null } else null,
                                    targetBarcodeLabel = if (missionType == "BARCODE") targetBarcodeLabel.trim().ifBlank { null } else null,
                                    targetBarcodeFormat = if (missionType == "BARCODE") targetBarcodeFormat else null,
                                    targetBarcodeLocation = if (missionType == "BARCODE") targetBarcodeLocation.trim().ifBlank { null } else null,
                                    registeredCodeId = if (missionType == "BARCODE") registeredCodeId else null,
                                    chainedBackupAlarmEnabled = chainedBackupAlarmEnabled,
                                    createdAt = alarm?.createdAt ?: System.currentTimeMillis()
                                )
                                onSave(savedEntity)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("save_custom_alarm_btn")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Alarm")
                        }
                    }
                }
            }
        }
    }

    // Household Item Camera Scanner Modal
    if (showHouseholdScanner) {
        BarcodeScannerView(
            scannerMode = ScannerMode.REGISTER_EXISTING,
            existingCodes = registeredCodes.map { it.codeValue },
            onCodeRegistered = { value, format, label, location ->
                targetBarcode = value
                targetBarcodeFormat = format
                targetBarcodeLabel = label
                targetBarcodeLocation = location ?: ""

                val newEntity = RegisteredCodeEntity(
                    codeValue = value,
                    codeFormat = format,
                    label = label,
                    locationHint = location,
                    isCustomGenerated = false,
                    createdAt = System.currentTimeMillis()
                )
                onSaveNewRegisteredCode?.invoke(newEntity)

                showHouseholdScanner = false
                testSuccessMessage = "Registered '$label' ($format)"
            },
            onCancelOrSkip = { showHouseholdScanner = false }
        )
    }

    // Test Scan Modal
    if (showTestScan && targetBarcode.isNotBlank()) {
        BarcodeScannerView(
            targetBarcode = targetBarcode,
            targetLabel = targetBarcodeLabel,
            targetFormat = targetBarcodeFormat,
            scannerMode = ScannerMode.DISMISS_VERIFY,
            onBarcodeMatched = {
                testSuccessMessage = "✓ Verified: '$targetBarcodeLabel' matches successfully!"
                showTestScan = false
            },
            onCancelOrSkip = { showTestScan = false }
        )
    }

    // Pick from Library Sheet
    if (showCodesPickerSheet && registeredCodes.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showCodesPickerSheet = false },
            title = { Text("Select Registered Code") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    registeredCodes.forEach { item ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    targetBarcode = item.codeValue
                                    targetBarcodeFormat = item.codeFormat
                                    targetBarcodeLabel = item.label
                                    targetBarcodeLocation = item.locationHint ?: ""
                                    registeredCodeId = item.id
                                    showCodesPickerSheet = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    if (!item.locationHint.isNullOrBlank()) {
                                        Text("📍 ${item.locationHint}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = item.codeFormat,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCodesPickerSheet = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Live, realistic phone lockscreen preview mockup showing the exact background wallpaper,
 * digital clock in the chosen typography, alarm label, and mission cues.
 */
@Composable
private fun AlarmLiveScreenPreviewMockup(
    title: String,
    timeFormatted: String,
    fontType: String,
    presetColor: String,
    customBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    missionType: String,
    missionLabel: String,
    audioTitle: String,
    crescendoSeconds: Int,
    isCompactHeight: Boolean
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isCompactHeight) 112.dp else 175.dp)
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Layer
            if (customBitmap != null) {
                Image(
                    bitmap = customBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.50f))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(getPresetBrush(presetColor))
                )
            }

            // Top Status Bar of Phone Mockup
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title.ifBlank { "Morning Rise" },
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    Surface(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "LIVE LOCKSCREEN PREVIEW",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Center: Big Time in Selected Font
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val clockFontFamily = getClockFontFamily(fontType)
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = clockFontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }

                // Bottom: Mission & Audio Cue Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            if (missionType == "BARCODE") {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = Color(0xFF64B5F6),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (missionLabel.isNotBlank()) "Scan: $missionLabel" else "Scan Target QR",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            } else {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color(0xFF81C784),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Swipe to Dismiss",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Surface(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                Icons.Default.Audiotrack,
                                contentDescription = null,
                                tint = Color(0xFFFFB74D),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val crescendoText = if (crescendoSeconds > 0) "${crescendoSeconds}s fade" else "instant"
                            Text(
                                text = "$audioTitle ($crescendoText)",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 9.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimeAndScheduleTabContent(
    title: String,
    onTitleChange: (String) -> Unit,
    displayHour12: Int,
    minute: Int,
    isPm: Boolean,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onToggleAmPm: (Boolean) -> Unit,
    repeatScheduleType: String,
    onRepeatScheduleTypeChange: (String) -> Unit,
    selectedDays: Set<String>,
    onToggleDay: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Label Input
        item {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Alarm Label") },
                placeholder = { Text("e.g. Deep Morning Rise") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("alarm_title_input")
            )
        }

        // Modern Touch-Friendly Time Picker
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Set Wake-Up Time",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Hour Selector Pill
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val nextH = if (displayHour12 >= 12) 1 else displayHour12 + 1
                                        onHourChange(nextH)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Hour +", modifier = Modifier.size(18.dp))
                                }

                                Text(
                                    text = String.format(java.util.Locale.US, "%02d", displayHour12),
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                IconButton(
                                    onClick = {
                                        val prevH = if (displayHour12 <= 1) 12 else displayHour12 - 1
                                        onHourChange(prevH)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Hour -", modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Text(
                            text = ":",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        // Minute Selector Pill
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val nextM = (minute + 5) % 60
                                        onMinuteChange(nextM)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Minute +5", modifier = Modifier.size(18.dp))
                                }

                                Text(
                                    text = String.format(java.util.Locale.US, "%02d", minute),
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                IconButton(
                                    onClick = {
                                        val prevM = if (minute < 5) 55 else minute - 5
                                        onMinuteChange(prevM)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Minute -5", modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // AM / PM Segmented Switcher
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (!isPm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (!isPm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .clickable { onToggleAmPm(false) }
                                    .testTag("alarm_select_am")
                            ) {
                                Text(
                                    text = "AM",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (!isPm) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isPm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isPm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .clickable { onToggleAmPm(true) }
                                    .testTag("alarm_select_pm")
                            ) {
                                Text(
                                    text = "PM",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isPm) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Time Preset Shortcuts
                    Text(
                        text = "Quick Presets",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val presets = listOf(
                            "6:00 AM" to (6 to 0 to false),
                            "6:30 AM" to (6 to 30 to false),
                            "7:00 AM" to (7 to 0 to false),
                            "7:30 AM" to (7 to 30 to false),
                            "8:00 AM" to (8 to 0 to false),
                            "+15m" to (-1 to 15 to false),
                            "+30m" to (-1 to 30 to false)
                        )
                        items(presets) { (label, config) ->
                            val (hourMin, pmFlag) = config
                            val (presetH, presetM) = hourMin
                            FilledTonalButton(
                                onClick = {
                                    if (presetH == -1) {
                                        // Relative addition
                                        val totalMin = (displayHour12 % 12) * 60 + minute + presetM
                                        val newH = (totalMin / 60)
                                        val finalH = if (newH == 0) 12 else newH
                                        onHourChange(finalH)
                                        onMinuteChange(totalMin % 60)
                                    } else {
                                        onHourChange(presetH)
                                        onMinuteChange(presetM)
                                        onToggleAmPm(pmFlag)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(label, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Schedule Repeat Days Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Schedule Days",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "ONCE" to "Ring Once",
                            "WEEKDAYS" to "Weekdays (Mon-Fri)",
                            "WEEKENDS" to "Weekends (Sat-Sun)",
                            "DAILY" to "Every Day",
                            "CUSTOM" to "Custom Days"
                        ).forEach { (typeId, label) ->
                            FilterChip(
                                selected = repeatScheduleType == typeId,
                                onClick = { onRepeatScheduleTypeChange(typeId) },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    if (repeatScheduleType == "CUSTOM") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(
                                "MON" to "M",
                                "TUE" to "T",
                                "WED" to "W",
                                "THU" to "T",
                                "FRI" to "F",
                                "SAT" to "S",
                                "SUN" to "S"
                            ).forEach { (dayCode, letter) ->
                                val isSelected = selectedDays.contains(dayCode)
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable { onToggleDay(dayCode) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = letter,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WallpaperAndStyleTabContent(
    wallpaperUri: String?,
    wallpaperPresetColor: String,
    timeClockFont: String,
    onSelectPresetColor: (String) -> Unit,
    onPickCustomImage: () -> Unit,
    onResetToPreset: () -> Unit,
    onSelectClockFont: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Preset Wallpapers Header
        item {
            Text(
                text = "Preset Gradient Wallpapers",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Select a rich, high-contrast atmospheric background for your wake-up screen",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Wallpaper Preset Cards Grid
        item {
            val presets = listOf(
                Triple("SUNRISE_GOLD", "Sunrise Gold", "Energizing warm golden hour dawn"),
                Triple("FOREST_EMERALD", "Forest Emerald", "Calming bamboo green mist"),
                Triple("COSMIC_VIOLET", "Cosmic Violet", "Deep celestial twilight flow"),
                Triple("MIDNIGHT_INDIGO", "Midnight Indigo", "Deep navy space tranquility"),
                Triple("DEEP_CRIMSON", "Deep Crimson", "Warm glowing sunset embers"),
                Triple("OBSIDIAN", "Pure Obsidian", "OLED pitch black distraction-free")
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                presets.forEach { (id, name, desc) ->
                    val isSelected = wallpaperUri.isNullOrBlank() && wallpaperPresetColor == id
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPresetColor(id) }
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .background(getPresetBrush(id))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.75f)
                                    )
                                }

                                if (isSelected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Custom Photo Wallpaper Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (!wallpaperUri.isNullOrBlank()) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = if (!wallpaperUri.isNullOrBlank()) 2.dp else 1.dp,
                    color = if (!wallpaperUri.isNullOrBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Personal Gallery Photo",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (!wallpaperUri.isNullOrBlank()) "Active photo selected from device" else "Pick custom image or wallpaper",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row {
                        if (!wallpaperUri.isNullOrBlank()) {
                            TextButton(onClick = onResetToPreset) {
                                Text("Remove", color = MaterialTheme.colorScheme.error)
                            }
                        }
                        Button(
                            onClick = onPickCustomImage,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Select")
                        }
                    }
                }
            }
        }

        // Clock Typography Selector Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Clock Typography Style",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val fontOptions = listOf(
                        Triple("MODERN_DIGITAL", "Modern Digital", FontFamily.Default),
                        Triple("MONOSPACE", "Monospace Tech", FontFamily.Monospace),
                        Triple("SERIF_ELEGANT", "Serif Elegant", FontFamily.Serif),
                        Triple("NEO_GROTESQUE", "Neo-Grotesque", FontFamily.SansSerif)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        fontOptions.forEach { (id, label, family) ->
                            val isSelected = timeClockFont == id
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelectClockFont(id) }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = "07:00",
                                        fontFamily = family,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = label.split(" ").first(),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AudioAndMissionTabContent(
    customAudioTitle: String,
    customAudioUri: String?,
    isPreviewPlaying: Boolean,
    onTogglePreview: () -> Unit,
    onPickAudioFile: () -> Unit,
    crescendoSeconds: Int,
    onCrescendoChange: (Int) -> Unit,
    isLooping: Boolean,
    onLoopChange: (Boolean) -> Unit,
    vibrateEnabled: Boolean,
    onVibrateChange: (Boolean) -> Unit,
    missionType: String,
    onMissionTypeChange: (String) -> Unit,
    targetBarcode: String,
    targetBarcodeLabel: String,
    targetBarcodeFormat: String,
    targetBarcodeLocation: String,
    onScanHouseholdItem: () -> Unit,
    onPickFromLibrary: () -> Unit,
    onTestScan: () -> Unit,
    onChangeBarcode: () -> Unit,
    onManualBarcodeChange: (String, String) -> Unit,
    testSuccessMessage: String?,
    registeredCodesCount: Int,
    chainedBackupAlarmEnabled: Boolean,
    onChainedBackupChange: (Boolean) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Audio & Crescendo Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Audio & Crescendo Volume",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            IconButton(onClick = onTogglePreview) {
                                Icon(
                                    imageVector = if (isPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "Preview Tone",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = customAudioTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (!customAudioUri.isNullOrBlank()) "Custom device audio" else "Built-in wake-up chime",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onPickAudioFile,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pick File", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Crescendo Fade-in Window
                    Text(
                        text = "Gentle Volume Fade-In (Crescendo)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            0 to "Instant Full",
                            30 to "30 sec",
                            60 to "60 sec",
                            120 to "2 min"
                        ).forEach { (sec, label) ->
                            FilterChip(
                                selected = crescendoSeconds == sec,
                                onClick = { onCrescendoChange(sec) },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Continuous Audio Loop", fontSize = 13.sp)
                        Switch(checked = isLooping, onCheckedChange = onLoopChange)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Vibration", fontSize = 13.sp)
                        Switch(checked = vibrateEnabled, onCheckedChange = onVibrateChange)
                    }
                }
            }
        }

        // Dismissal Mission Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Dismissal Mission",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = missionType == "NONE",
                            onClick = { onMissionTypeChange("NONE") },
                            label = { Text("Swipe to Dismiss") },
                            leadingIcon = { Icon(Icons.Default.Swipe, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = missionType == "BARCODE",
                            onClick = { onMissionTypeChange("BARCODE") },
                            label = { Text("QR / Barcode CAPTCHA") },
                            leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    if (missionType == "BARCODE") {
                        Spacer(modifier = Modifier.height(10.dp))

                        if (testSuccessMessage != null) {
                            Surface(
                                color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(testSuccessMessage, color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (targetBarcode.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = targetBarcodeLabel.ifBlank { "Household Target Code" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            if (targetBarcodeLocation.isNotBlank()) {
                                                Text(
                                                    text = "📍 $targetBarcodeLocation",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = targetBarcodeFormat,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = targetBarcode,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        OutlinedButton(onClick = onTestScan) {
                                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Test Scan", fontSize = 11.sp)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        TextButton(onClick = onChangeBarcode) {
                                            Text("Change", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = onScanHouseholdItem,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Scan Household Item with Camera")
                                }

                                if (registeredCodesCount > 0) {
                                    OutlinedButton(
                                        onClick = onPickFromLibrary,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Choose from Saved Library ($registeredCodesCount items)")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Chained Backup Alarm
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Chained Backup Alarm (+5 min)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Fires aggressive high-priority fallback alarm if mission is left unsolved",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = chainedBackupAlarmEnabled,
                            onCheckedChange = onChainedBackupChange
                        )
                    }
                }
            }
        }
    }
}
