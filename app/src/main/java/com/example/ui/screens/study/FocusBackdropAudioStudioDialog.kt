package com.example.ui.screens.study

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.MainViewModel
import com.example.util.ConcentrationSoundscapeHelper
import com.example.util.SoundscapeType
import com.example.util.StudyMusicPlayer
import com.example.util.StudySoundHelper
import com.example.util.StudySoundType

/**
 * Redesigned Focus Studio: An atmospheric, calm sanctuary for tailoring
 * study soundscapes, local audio files, ambient backdrops, audio signals,
 * and environmental focus controls.
 *
 * Designed with OLED-friendly styling and muted sage/mint accents.
 */
@Composable
fun FocusBackdropAudioStudioDialog(
    viewModel: MainViewModel? = null,
    currentTheme: FocusAestheticTheme = FocusAestheticTheme.MINIMAL_OLED,
    onThemeSelected: (FocusAestheticTheme) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    // Soundscapes State
    val currentSoundscape by ConcentrationSoundscapeHelper.currentSoundscape.collectAsStateWithLifecycle()
    val isSoundscapePlaying by ConcentrationSoundscapeHelper.isPlaying.collectAsStateWithLifecycle()
    val soundscapeVolume by ConcentrationSoundscapeHelper.volume.collectAsStateWithLifecycle()

    // Local Music Player State
    val musicTrackTitle by StudyMusicPlayer.currentTrackTitle.collectAsStateWithLifecycle()
    val musicUri by StudyMusicPlayer.currentTrackUri.collectAsStateWithLifecycle()
    val isMusicPlaying by StudyMusicPlayer.isPlaying.collectAsStateWithLifecycle()
    val musicPosMs by StudyMusicPlayer.currentPositionMs.collectAsStateWithLifecycle()
    val musicDurationMs by StudyMusicPlayer.durationMs.collectAsStateWithLifecycle()
    val musicVolume by StudyMusicPlayer.volume.collectAsStateWithLifecycle()
    val isMusicLooping by StudyMusicPlayer.isLooping.collectAsStateWithLifecycle()
    val autoPlayMusicWithTimer by StudyMusicPlayer.autoPlayWithTimer.collectAsStateWithLifecycle()

    // Background State (from ViewModel if available)
    val bgType = viewModel?.focusBgType?.collectAsStateWithLifecycle()?.value ?: FocusBgType.BUILTIN
    val bgBuiltinId = viewModel?.focusBgBuiltinId?.collectAsStateWithLifecycle()?.value ?: currentTheme.id
    val customUri = viewModel?.focusBgCustomUri?.collectAsStateWithLifecycle()?.value
    val overlayOpacity = viewModel?.focusBgOverlayOpacity?.collectAsStateWithLifecycle()?.value ?: 0.60f
    val brightness = viewModel?.focusBgBrightness?.collectAsStateWithLifecycle()?.value ?: 0.85f
    val isBlur = viewModel?.focusBgBlur?.collectAsStateWithLifecycle()?.value ?: false
    val fitMode = viewModel?.focusBgFit?.collectAsStateWithLifecycle()?.value ?: "CROP"
    val isAmbientAnimation = viewModel?.focusAmbientAnimationEnabled?.collectAsStateWithLifecycle()?.value ?: true

    // Audio Cues & Screen Awake States
    val startSoundEnabled = viewModel?.studyStartSoundEnabled?.collectAsStateWithLifecycle()?.value ?: true
    val breakStartSoundEnabled = viewModel?.studyBreakStartSoundEnabled?.collectAsStateWithLifecycle()?.value ?: true
    val breakEndSoundEnabled = viewModel?.studyBreakEndSoundEnabled?.collectAsStateWithLifecycle()?.value ?: true
    val endSoundEnabled = viewModel?.studyEndSoundEnabled?.collectAsStateWithLifecycle()?.value ?: true
    val keepScreenOn = viewModel?.studyKeepScreenOnEnabled?.collectAsStateWithLifecycle()?.value ?: false

    // SAF Picker for Local Music
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            StudyMusicPlayer.loadLocalTrack(context, it)
        }
    }

    // Android Photo Picker for Custom Gallery Background
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            try {
                val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(pickedUri, flag)
            } catch (_: Exception) {
                // Ignore if provider doesn't support persistable flags
            }
            viewModel?.setFocusBgCustomUri(pickedUri.toString())
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .heightIn(max = 720.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0C100D), // OLED near-black surface
            tonalElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223326))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Focus Studio",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color(0xFFE8ECE9) // Soft white
                        )
                        Text(
                            text = "Calm soundscapes, music & ambient background",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8F9E94) // Muted sage grey
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_studio_dialog_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF8F9E94))
                    }
                }

                // 5 Tab Sections
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF080C09),
                    contentColor = Color(0xFFA8C9B0),
                    edgePadding = 12.dp
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Soundscapes", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        selectedContentColor = Color(0xFFA8C9B0),
                        unselectedContentColor = Color(0xFF6B7A70)
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("My Music", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        selectedContentColor = Color(0xFFA8C9B0),
                        unselectedContentColor = Color(0xFF6B7A70)
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Backgrounds", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.ColorLens, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        selectedContentColor = Color(0xFFA8C9B0),
                        unselectedContentColor = Color(0xFF6B7A70)
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Audio Cues", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        selectedContentColor = Color(0xFFA8C9B0),
                        unselectedContentColor = Color(0xFF6B7A70)
                    )
                    Tab(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        text = { Text("Environment", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        selectedContentColor = Color(0xFFA8C9B0),
                        unselectedContentColor = Color(0xFF6B7A70)
                    )
                }

                // Tab Content Body
                Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                    when (selectedTab) {
                        0 -> SoundscapesTab(
                            currentSoundscape = currentSoundscape,
                            isPlaying = isSoundscapePlaying,
                            volume = soundscapeVolume,
                            onSelect = { ConcentrationSoundscapeHelper.selectSoundscape(it, autoPlay = true) },
                            onTogglePlay = { ConcentrationSoundscapeHelper.togglePlayPause() },
                            onVolumeChange = { ConcentrationSoundscapeHelper.setVolume(it) }
                        )

                        1 -> MyMusicTab(
                            trackTitle = musicTrackTitle,
                            hasTrack = musicUri != null,
                            isPlaying = isMusicPlaying,
                            positionMs = musicPosMs,
                            durationMs = musicDurationMs,
                            volume = musicVolume,
                            isLooping = isMusicLooping,
                            autoPlayWithTimer = autoPlayMusicWithTimer,
                            onPickFile = { audioPickerLauncher.launch("audio/*") },
                            onTogglePlay = { StudyMusicPlayer.togglePlayPause() },
                            onSeek = { StudyMusicPlayer.seekTo(it) },
                            onVolumeChange = { StudyMusicPlayer.setVolume(it) },
                            onToggleLoop = { StudyMusicPlayer.setLooping(!isMusicLooping) },
                            onToggleAutoPlay = { StudyMusicPlayer.setAutoPlayWithTimer(!autoPlayMusicWithTimer) },
                            onClearTrack = { StudyMusicPlayer.clearTrack() }
                        )

                        2 -> ThemesAndBackgroundsTab(
                            bgType = bgType,
                            builtinId = bgBuiltinId,
                            customUri = customUri,
                            overlayOpacity = overlayOpacity,
                            brightness = brightness,
                            isBlur = isBlur,
                            fitMode = fitMode,
                            onSelectOled = {
                                viewModel?.setFocusBgType(FocusBgType.OLED)
                                onThemeSelected(FocusAestheticTheme.MINIMAL_OLED)
                            },
                            onSelectBuiltin = { theme ->
                                viewModel?.setFocusBgBuiltinId(theme.id)
                                onThemeSelected(theme)
                            },
                            onPickCustom = {
                                photoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            onClearCustom = {
                                viewModel?.clearFocusBgCustomUri()
                            },
                            onOverlayOpacityChange = { viewModel?.setFocusBgOverlayOpacity(it) },
                            onBrightnessChange = { viewModel?.setFocusBgBrightness(it) },
                            onToggleBlur = { viewModel?.setFocusBgBlur(it) },
                            onToggleFitMode = {
                                viewModel?.setFocusBgFit(if (fitMode == "CROP") "FIT" else "CROP")
                            }
                        )

                        3 -> AudioCuesTab(
                            startSoundEnabled = startSoundEnabled,
                            breakStartSoundEnabled = breakStartSoundEnabled,
                            breakEndSoundEnabled = breakEndSoundEnabled,
                            endSoundEnabled = endSoundEnabled,
                            onToggleStartSound = { viewModel?.setStudyStartSoundEnabled(it) },
                            onToggleBreakStartSound = { viewModel?.setStudyBreakStartSoundEnabled(it) },
                            onToggleBreakEndSound = { viewModel?.setStudyBreakEndSoundEnabled(it) },
                            onToggleEndSound = { viewModel?.setStudyEndSoundEnabled(it) },
                            onPreview = { type -> StudySoundHelper.previewSound(context, type) }
                        )

                        4 -> EnvironmentTab(
                            keepScreenOn = keepScreenOn,
                            onToggleKeepScreenOn = { viewModel?.setStudyKeepScreenOn(it) },
                            isAmbientAnimation = isAmbientAnimation,
                            onToggleAmbientAnimation = { viewModel?.setFocusAmbientAnimationEnabled(it) }
                        )
                    }
                }

                // Footer Apply Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFA8C9B0), // Soft sage
                            contentColor = Color(0xFF08140B)
                        ),
                        modifier = Modifier.testTag("done_studio_button")
                    ) {
                        Text("Apply & Focus", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 1. SOUNDSCAPES TAB
// -------------------------------------------------------------
@Composable
private fun SoundscapesTab(
    currentSoundscape: SoundscapeType,
    isPlaying: Boolean,
    volume: Float,
    onSelect: (SoundscapeType) -> Unit,
    onTogglePlay: () -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131A15)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF202C23)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Active Soundscape",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA8C9B0)
                            )
                            Text(
                                text = "${currentSoundscape.iconEmoji} ${currentSoundscape.displayName}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        FilledTonalButton(
                            onClick = onTogglePlay,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF1E2B21),
                                contentColor = Color(0xFFA8C9B0)
                            )
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isPlaying) "Pause" else "Play", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.VolumeDown,
                            contentDescription = null,
                            tint = Color(0xFF8F9E94),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = volume,
                            onValueChange = onVolumeChange,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFA8C9B0),
                                activeTrackColor = Color(0xFFA8C9B0),
                                inactiveTrackColor = Color(0xFF243026)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = Color(0xFF8F9E94),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        items(SoundscapeType.values()) { item ->
            val isSelected = currentSoundscape == item
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF18261C) else Color(0xFF0F1411)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) Color(0xFFA8C9B0) else Color(0xFF1B241E)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(item) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.iconEmoji,
                            fontSize = 24.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.displayName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isSelected) Color(0xFFA8C9B0) else Color(0xFFE2E8E4)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = Color(0xFF1A231C),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = item.vibeTag,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = Color(0xFF8F9E94),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF7A897F)
                            )
                        }
                    }

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFA8C9B0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Active",
                                tint = Color(0xFF0B170E),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. MY MUSIC TAB
// -------------------------------------------------------------
@Composable
private fun MyMusicTab(
    trackTitle: String?,
    hasTrack: Boolean,
    isPlaying: Boolean,
    positionMs: Int,
    durationMs: Int,
    volume: Float,
    isLooping: Boolean,
    autoPlayWithTimer: Boolean,
    onPickFile: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Int) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleLoop: () -> Unit,
    onToggleAutoPlay: () -> Unit,
    onClearTrack: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131A15)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF202C23)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Default.Audiotrack,
                                contentDescription = null,
                                tint = Color(0xFFA8C9B0),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Device Music / Audio File",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA8C9B0)
                                )
                                Text(
                                    text = trackTitle ?: "No Music Loaded",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onPickFile,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA8C9B0).copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA8C9B0))
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select File", fontSize = 12.sp)
                        }
                    }

                    if (hasTrack) {
                        Spacer(modifier = Modifier.height(16.dp))

                        val currentSec = positionMs / 1000
                        val totalSec = durationMs / 1000
                        val posString = String.format(java.util.Locale.getDefault(), "%02d:%02d", currentSec / 60, currentSec % 60)
                        val durString = String.format(java.util.Locale.getDefault(), "%02d:%02d", totalSec / 60, totalSec % 60)

                        Slider(
                            value = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f,
                            onValueChange = { frac -> onSeek((frac * durationMs).toInt()) },
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFA8C9B0),
                                activeTrackColor = Color(0xFFA8C9B0),
                                inactiveTrackColor = Color(0xFF243026)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = posString, style = MaterialTheme.typography.labelSmall, color = Color(0xFF8F9E94))
                            Text(text = durString, style = MaterialTheme.typography.labelSmall, color = Color(0xFF8F9E94))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onToggleLoop) {
                                Icon(
                                    Icons.Default.Repeat,
                                    contentDescription = "Loop Song",
                                    tint = if (isLooping) Color(0xFFA8C9B0) else Color(0xFF6B7A70)
                                )
                            }

                            Button(
                                onClick = onTogglePlay,
                                shape = CircleShape,
                                modifier = Modifier.size(52.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA8C9B0), contentColor = Color(0xFF08140B))
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            OutlinedButton(
                                onClick = onClearTrack,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334437)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8F9E94))
                            ) {
                                Text("Clear", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeDown, contentDescription = null, tint = Color(0xFF8F9E94), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Slider(
                                value = volume,
                                onValueChange = onVolumeChange,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFA8C9B0),
                                    activeTrackColor = Color(0xFFA8C9B0),
                                    inactiveTrackColor = Color(0xFF243026)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFF8F9E94), modifier = Modifier.size(18.dp))
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Select any local MP3, M4A, or WAV file from your device to accompany your focus sprints.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7A897F)
                        )
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1411)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1B241E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sync Music with Focus Timer",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                        Text(
                            text = "Automatically start music when study timer starts, and pause when paused.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7A897F)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = autoPlayWithTimer,
                        onCheckedChange = { onToggleAutoPlay() },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF08140B),
                            checkedTrackColor = Color(0xFFA8C9B0)
                        )
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. THEMES & BACKGROUNDS TAB
// -------------------------------------------------------------
@Composable
private fun ThemesAndBackgroundsTab(
    bgType: FocusBgType,
    builtinId: String,
    customUri: String?,
    overlayOpacity: Float,
    brightness: Float,
    isBlur: Boolean,
    fitMode: String,
    onSelectOled: () -> Unit,
    onSelectBuiltin: (FocusAestheticTheme) -> Unit,
    onPickCustom: () -> Unit,
    onClearCustom: () -> Unit,
    onOverlayOpacityChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onToggleBlur: (Boolean) -> Unit,
    onToggleFitMode: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Custom Gallery Photo Section
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131A15)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF202C23)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Personal Gallery Background",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Use your favorite calming photo from phone gallery",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF8F9E94)
                            )
                        }

                        Button(
                            onClick = onPickCustom,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFA8C9B0),
                                contentColor = Color(0xFF08140B)
                            )
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (customUri.isNullOrBlank()) "Choose Photo" else "Replace Photo", fontSize = 12.sp)
                        }
                    }

                    if (!customUri.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFFA8C9B0), RoundedCornerShape(12.dp))
                            ) {
                                AsyncImage(
                                    model = Uri.parse(customUri),
                                    contentDescription = "Custom preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (bgType == FocusBgType.CUSTOM) "Currently Active Background" else "Saved Photo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA8C9B0)
                                )
                                Text(
                                    text = "Custom wallpaper applied",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = onClearCustom,
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF442222)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171))
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Remove", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Appearance Adjustments Controls (Dimming, Opacity, Blur, Fit)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1411)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1B241E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Background Presentation Controls",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFA8C9B0)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dark Overlay Opacity
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dark Overlay Opacity (${(overlayOpacity * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8E4)
                        )
                        Text(text = "Enhances text legibility", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B7A70))
                    }
                    Slider(
                        value = overlayOpacity,
                        onValueChange = onOverlayOpacityChange,
                        valueRange = 0.20f..0.90f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFA8C9B0),
                            activeTrackColor = Color(0xFFA8C9B0),
                            inactiveTrackColor = Color(0xFF243026)
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Brightness Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Background Brightness (${(brightness * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8E4)
                        )
                        Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color(0xFF8F9E94), modifier = Modifier.size(16.dp))
                    }
                    Slider(
                        value = brightness,
                        onValueChange = onBrightnessChange,
                        valueRange = 0.30f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFA8C9B0),
                            activeTrackColor = Color(0xFFA8C9B0),
                            inactiveTrackColor = Color(0xFF243026)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Blur & Fit Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Subtle Soft Blur", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                        Switch(
                            checked = isBlur,
                            onCheckedChange = onToggleBlur,
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF08140B),
                                checkedTrackColor = Color(0xFFA8C9B0)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Aspect Ratio Scale Mode", style = MaterialTheme.typography.bodySmall, color = Color.White)
                            Text("Current: $fitMode", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8F9E94))
                        }
                        OutlinedButton(
                            onClick = onToggleFitMode,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334437))
                        ) {
                            Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (fitMode == "CROP") "Fill / Crop" else "Fit Screen", fontSize = 11.sp, color = Color(0xFFA8C9B0))
                        }
                    }
                }
            }
        }

        // Curated Built-in Atmospheric Backgrounds Header
        item {
            Text(
                text = "Curated Atmospheric Backdrops",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
        }

        // List of all 10 Curated Themes
        items(FocusAestheticTheme.values()) { theme ->
            val isSelected = (bgType == FocusBgType.BUILTIN || bgType == FocusBgType.OLED) && builtinId.equals(theme.id, ignoreCase = true)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = theme.cardBackground),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) theme.primaryAccent else Color.White.copy(alpha = 0.10f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (theme == FocusAestheticTheme.MINIMAL_OLED) {
                            onSelectOled()
                        } else {
                            onSelectBuiltin(theme)
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(theme.primaryAccent)
                            )
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(theme.secondaryAccent)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = theme.displayName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = theme.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9EABA2)
                            )
                        }
                    }

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(theme.primaryAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. AUDIO CUES TAB
// -------------------------------------------------------------
@Composable
private fun AudioCuesTab(
    startSoundEnabled: Boolean,
    breakStartSoundEnabled: Boolean,
    breakEndSoundEnabled: Boolean,
    endSoundEnabled: Boolean,
    onToggleStartSound: (Boolean) -> Unit,
    onToggleBreakStartSound: (Boolean) -> Unit,
    onToggleBreakEndSound: (Boolean) -> Unit,
    onToggleEndSound: (Boolean) -> Unit,
    onPreview: (StudySoundType) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Gentle Auditory Signals",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = "Calming chime signals to bookend your study sprints and micro-breaks without startling you.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8F9E94)
            )
        }

        item {
            AudioCueCard(
                title = "Session Start Bell",
                description = "Pleasant deep gong signaling the start of a focus sprint",
                isEnabled = startSoundEnabled,
                onToggle = onToggleStartSound,
                onPreview = { onPreview(StudySoundType.SESSION_START) }
            )
        }

        item {
            AudioCueCard(
                title = "Break Start Chime",
                description = "Soft ascending tone when entering a micro-break or recharge",
                isEnabled = breakStartSoundEnabled,
                onToggle = onToggleBreakStartSound,
                onPreview = { onPreview(StudySoundType.BREAK_START) }
            )
        }

        item {
            AudioCueCard(
                title = "Break Ending Notice",
                description = "Gentle double chime 30 seconds before break ends",
                isEnabled = breakEndSoundEnabled,
                onToggle = onToggleBreakEndSound,
                onPreview = { onPreview(StudySoundType.BREAK_END) }
            )
        }

        item {
            AudioCueCard(
                title = "Session Completed Bell",
                description = "Warm celebratory chord when reaching target focus time",
                isEnabled = endSoundEnabled,
                onToggle = onToggleEndSound,
                onPreview = { onPreview(StudySoundType.SESSION_END) }
            )
        }
    }
}

@Composable
private fun AudioCueCard(
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onPreview: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1411)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1B241E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = Color(0xFF7A897F))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onPreview,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334437))
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFA8C9B0))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test", fontSize = 11.sp, color = Color(0xFFA8C9B0))
                }

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = androidx.compose.material3.SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF08140B),
                        checkedTrackColor = Color(0xFFA8C9B0)
                    )
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 5. ENVIRONMENT TAB
// -------------------------------------------------------------
@Composable
private fun EnvironmentTab(
    keepScreenOn: Boolean,
    onToggleKeepScreenOn: (Boolean) -> Unit,
    isAmbientAnimation: Boolean,
    onToggleAmbientAnimation: (Boolean) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Focus Room Environment",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = "Tune display and breathing motion to match your ADHD and energy requirements.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8F9E94)
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1411)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1B241E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Keep Screen Awake",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                        Text(
                            text = "Prevents device display from sleeping while a focus sprint or break is actively running.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7A897F)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = keepScreenOn,
                        onCheckedChange = onToggleKeepScreenOn,
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF08140B),
                            checkedTrackColor = Color(0xFFA8C9B0)
                        )
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1411)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1B241E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ambient Breathing Motion",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                        Text(
                            text = "Slow, rhythmic pulse in ambient background orbs. Disable for completely static stillness.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7A897F)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = isAmbientAnimation,
                        onCheckedChange = onToggleAmbientAnimation,
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF08140B),
                            checkedTrackColor = Color(0xFFA8C9B0)
                        )
                    )
                }
            }
        }
    }
}
