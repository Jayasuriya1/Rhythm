package com.example.ui.screens.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import com.example.data.database.AppDatabase
import com.example.data.entity.CustomAlarmEntity
import com.example.service.CustomAlarmPlaybackService
import com.example.ui.theme.RhythmFocusTheme
import com.example.util.CustomAlarmScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class ActiveAlarmActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ALARM_ID = "extra_active_alarm_id"
        const val EXTRA_IS_BACKUP = "extra_active_is_backup"
    }

    private var alarmId: Long = 0L
    private var isBackup: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        alarmId = intent.getLongExtra(EXTRA_ALARM_ID, 0L)
        isBackup = intent.getBooleanExtra(EXTRA_IS_BACKUP, false)

        setupLockScreenFlags()

        setContent {
            RhythmFocusTheme {
                ActiveAlarmScreen(
                    alarmId = alarmId,
                    isBackup = isBackup,
                    onDismissAlarm = {
                        dismissAndFinish()
                    }
                )
            }
        }
    }

    private fun setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /**
     * Behavioral Failsafe: Intercept physical volume buttons to prevent user
     * from muting the alarm before completing the mission.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
            keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
        ) {
            Toast.makeText(this, "Complete mission to dismiss alarm!", Toast.LENGTH_SHORT).show()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
            keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
        ) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun dismissAndFinish() {
        val stopIntent = Intent(this, CustomAlarmPlaybackService::class.java).apply {
            action = CustomAlarmPlaybackService.ACTION_STOP
            putExtra(CustomAlarmPlaybackService.EXTRA_ALARM_ID, alarmId)
        }
        startService(stopIntent)
        CustomAlarmScheduler.cancelChainedBackupAlarm(this, alarmId)
        Toast.makeText(this, "Alarm dismissed! Have a focused day.", Toast.LENGTH_SHORT).show()
        finish()
    }
}

@Composable
fun ActiveAlarmScreen(
    alarmId: Long,
    isBackup: Boolean,
    onDismissAlarm: () -> Unit
) {
    val context = LocalContext.current
    var alarm by remember { mutableStateOf<CustomAlarmEntity?>(null) }
    var showScannerView by remember { mutableStateOf(false) }

    // Live clock ticker
    var currentTimeString by remember { mutableStateOf("") }
    var currentDateString by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        // Query alarm from DB
        val db = AppDatabase.getDatabase(context)
        alarm = if (alarmId > 0) db.customAlarmDao().getAlarmByIdSync(alarmId) else null

        // Ticker loop
        while (true) {
            val now = Date()
            currentTimeString = SimpleDateFormat("hh:mm", Locale.getDefault()).format(now)
            val amPm = SimpleDateFormat("a", Locale.getDefault()).format(now).uppercase()
            currentTimeString = "$currentTimeString $amPm"
            currentDateString = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(now)
            delay(1000)
        }
    }

    // Behavioral Failsafe: Disable standard back button
    BackHandler {
        Toast.makeText(context, "Complete mission to dismiss alarm!", Toast.LENGTH_SHORT).show()
    }

    val wallpaperUri = alarm?.wallpaperUri
    val presetColor = alarm?.wallpaperPresetColor ?: "MIDNIGHT_INDIGO"
    val clockFont = alarm?.timeClockFont ?: "MODERN_DIGITAL"
    val missionType = alarm?.missionType ?: "NONE"

    val customBitmap = remember(wallpaperUri) {
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

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Custom Alarm Wallpaper Layer
        if (customBitmap != null) {
            Image(
                bitmap = customBitmap,
                contentDescription = "Custom Alarm Wallpaper",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Scrim overlay for high-contrast text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
            )
        } else {
            // Preset Gradient Wallpaper
            val gradientBrush = getPresetBrush(presetColor)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(gradientBrush)
            )
        }

        // 2. Backup Alarm Warning Banner
        if (isBackup) {
            Surface(
                color = Color(0xFFD32F2F),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Alert",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CHAINED BACKUP ALARM ACTIVE — WAKE UP!",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // 3. Layered Time Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Alarm Title & Date
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = if (isBackup) 32.dp else 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = "Alarm",
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = alarm?.title ?: "Wake-Up Alarm",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentDateString,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }

            // Center Section: Big High-Contrast Digital Clock Display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val chosenFontFamily = getClockFontFamily(clockFont)
                Text(
                    text = currentTimeString.ifBlank { "07:00 AM" },
                    color = Color.White,
                    fontSize = 68.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = chosenFontFamily,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp
                )

                if (alarm?.chainedBackupAlarmEnabled == true && !isBackup) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Chained backup alarm armed in 5m",
                        color = Color(0xFFFFB74D),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Bottom Section: Dismissal Mission Controls
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (missionType == "BARCODE") {
                    // Barcode CAPTCHA Mission Button
                    Button(
                        onClick = { showScannerView = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scanner",
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Scan Barcode to Dismiss",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!alarm?.targetBarcodeLabel.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Mission Target: ${alarm?.targetBarcodeLabel}",
                            color = Color(0xFFFFD54F),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    // Standard Swipe-To-Dismiss Slider
                    SwipeToDismissSlider(
                        onDismiss = onDismissAlarm
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                // Safety Emergency Override (Press & hold 10 seconds in case of extreme emergency / camera failure)
                EmergencyOverrideButton(onEmergencyDismiss = onDismissAlarm)
            }
        }

        // Camera Barcode Scanner Full-Screen Modal
        if (showScannerView) {
            BarcodeScannerView(
                targetBarcode = alarm?.targetBarcode,
                targetLabel = alarm?.targetBarcodeLabel,
                onBarcodeMatched = {
                    showScannerView = false
                    onDismissAlarm()
                },
                onCancelOrSkip = {
                    showScannerView = false
                }
            )
        }
    }
}

@Composable
fun SwipeToDismissSlider(
    onDismiss: () -> Unit
) {
    var isSwiped by remember { mutableStateOf(false) }

    Surface(
        color = Color.White.copy(alpha = 0.2f),
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "Tap or Slide to Dismiss  >>>",
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.Center)
            )

            Button(
                onClick = {
                    isSwiped = true
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = CircleShape,
                modifier = Modifier
                    .padding(4.dp)
                    .size(56.dp)
                    .align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Dismiss",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun EmergencyOverrideButton(
    onEmergencyDismiss: () -> Unit
) {
    var holdSeconds by remember { mutableIntStateOf(0) }
    var isHolding by remember { mutableStateOf(false) }

    LaunchedEffect(isHolding) {
        if (isHolding) {
            holdSeconds = 0
            while (isHolding && holdSeconds < 5) {
                delay(1000)
                holdSeconds++
            }
            if (isHolding && holdSeconds >= 5) {
                onEmergencyDismiss()
            }
        } else {
            holdSeconds = 0
        }
    }

    Button(
        onClick = {
            // Immediate tap triggers warning countdown hint
            if (holdSeconds < 5) {
                holdSeconds++
                if (holdSeconds >= 5) {
                    onEmergencyDismiss()
                }
            }
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = if (holdSeconds > 0) "Emergency: Tap ${5 - holdSeconds} more times to dismiss" else "Emergency Dismissal",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 12.sp
        )
    }
}

fun getPresetBrush(preset: String): Brush {
    return when (preset) {
        "SUNRISE_GOLD" -> Brush.verticalGradient(
            colors = listOf(Color(0xFF2E1C0C), Color(0xFFE65100), Color(0xFFFFB300))
        )
        "FOREST_EMERALD" -> Brush.verticalGradient(
            colors = listOf(Color(0xFF0D2818), Color(0xFF1B4332), Color(0xFF2D6A4F))
        )
        "DEEP_CRIMSON" -> Brush.verticalGradient(
            colors = listOf(Color(0xFF2B0909), Color(0xFF590D22), Color(0xFF800F2F))
        )
        "COSMIC_VIOLET" -> Brush.verticalGradient(
            colors = listOf(Color(0xFF10002B), Color(0xFF240046), Color(0xFF5A189A))
        )
        "OBSIDIAN" -> Brush.verticalGradient(
            colors = listOf(Color(0xFF121212), Color(0xFF1E1E1E), Color(0xFF2C2C2C))
        )
        else -> Brush.verticalGradient(
            // Midnight Indigo default
            colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))
        )
    }
}

fun getClockFontFamily(fontConfig: String): FontFamily {
    return when (fontConfig) {
        "MONOSPACE" -> FontFamily.Monospace
        "SERIF_ELEGANT" -> FontFamily.Serif
        "NEO_GROTESQUE" -> FontFamily.SansSerif
        else -> FontFamily.Default // Modern Digital
    }
}
