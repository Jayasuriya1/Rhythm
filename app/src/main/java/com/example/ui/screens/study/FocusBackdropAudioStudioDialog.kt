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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.MainViewModel
import com.example.util.ConcentrationSoundscapeHelper
import com.example.util.FocusWallpaperItem
import com.example.util.FocusWallpaperManager
import com.example.util.MusicRepeatMode
import com.example.util.SoundscapeType
import com.example.util.StudyMusicPlayer
import com.example.util.StudySoundHelper
import com.example.util.StudySoundType
import kotlinx.coroutines.launch
import java.io.File

/**
 * Focus Studio: The complete sanctuary for ambient soundscapes, offline music,
 * persistent personal wallpaper library, online music shortcuts, and environment controls.
 */
@Composable
fun FocusBackdropAudioStudioDialog(
    viewModel: MainViewModel? = null,
    currentTheme: FocusAestheticTheme = FocusAestheticTheme.MINIMAL_OLED,
    onThemeSelected: (FocusAestheticTheme) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    // Soundscapes State
    val currentSoundscape by ConcentrationSoundscapeHelper.currentSoundscape.collectAsStateWithLifecycle()
    val isSoundscapePlaying by ConcentrationSoundscapeHelper.isPlaying.collectAsStateWithLifecycle()
    val soundscapeVolume by ConcentrationSoundscapeHelper.volume.collectAsStateWithLifecycle()

    // Offline Music Player State
    val playlist by StudyMusicPlayer.playlist.collectAsStateWithLifecycle()
    val currentTrackIndex by StudyMusicPlayer.currentTrackIndex.collectAsStateWithLifecycle()
    val currentTrack by StudyMusicPlayer.currentTrack.collectAsStateWithLifecycle()
    val isMusicPlaying by StudyMusicPlayer.isPlaying.collectAsStateWithLifecycle()
    val musicPosMs by StudyMusicPlayer.currentPositionMs.collectAsStateWithLifecycle()
    val musicDurationMs by StudyMusicPlayer.durationMs.collectAsStateWithLifecycle()
    val musicVolume by StudyMusicPlayer.volume.collectAsStateWithLifecycle()
    val repeatMode by StudyMusicPlayer.repeatMode.collectAsStateWithLifecycle()
    val isShuffle by StudyMusicPlayer.isShuffle.collectAsStateWithLifecycle()
    val autoPlayMusicWithTimer by StudyMusicPlayer.autoPlayWithTimer.collectAsStateWithLifecycle()

    // Personal Wallpapers State
    val importedWallpapers by FocusWallpaperManager.wallpapers.collectAsStateWithLifecycle()
    val activeWallpaper by FocusWallpaperManager.activeWallpaper.collectAsStateWithLifecycle()

    // Background State from ViewModel
    val bgType = viewModel?.focusBgType?.collectAsStateWithLifecycle()?.value ?: FocusBgType.BUILTIN
    val bgBuiltinId = viewModel?.focusBgBuiltinId?.collectAsStateWithLifecycle()?.value ?: currentTheme.id
    val customUri = viewModel?.focusBgCustomUri?.collectAsStateWithLifecycle()?.value
    val overlayOpacity = viewModel?.focusBgOverlayOpacity?.collectAsStateWithLifecycle()?.value ?: 0.60f
    val brightness = viewModel?.focusBgBrightness?.collectAsStateWithLifecycle()?.value ?: 0.85f
    val isBlur = viewModel?.focusBgBlur?.collectAsStateWithLifecycle()?.value ?: false
    val fitMode = viewModel?.focusBgFit?.collectAsStateWithLifecycle()?.value ?: "CROP"
    val isAmbientAnimation = viewModel?.focusAmbientAnimationEnabled?.collectAsStateWithLifecycle()?.value ?: true
    val isSlideshow = viewModel?.focusSlideshowEnabled?.collectAsStateWithLifecycle()?.value ?: false
    val slideshowInterval = viewModel?.focusSlideshowIntervalMinutes?.collectAsStateWithLifecycle()?.value ?: 5

    // Environment & Preferences
    val backgroundAudioAllowed = viewModel?.studyBackgroundAudioEnabled?.collectAsStateWithLifecycle()?.value ?: false
    val immersiveFullscreenEnabled = viewModel?.studyImmersiveFullscreenEnabled?.collectAsStateWithLifecycle()?.value ?: true
    val keepScreenOn = viewModel?.studyKeepScreenOnEnabled?.collectAsStateWithLifecycle()?.value ?: false
    val startSoundEnabled = viewModel?.studyStartSoundEnabled?.collectAsStateWithLifecycle()?.value ?: true
    val breakStartSoundEnabled = viewModel?.studyBreakStartSoundEnabled?.collectAsStateWithLifecycle()?.value ?: true
    val breakEndSoundEnabled = viewModel?.studyBreakEndSoundEnabled?.collectAsStateWithLifecycle()?.value ?: true
    val endSoundEnabled = viewModel?.studyEndSoundEnabled?.collectAsStateWithLifecycle()?.value ?: true

    // File pickers
    val multiAudioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            StudyMusicPlayer.addTracks(context, uris)
        }
    }

    val multiPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                val newlyImported = FocusWallpaperManager.importWallpapers(context, uris)
                viewModel?.saveFocusWallpapersList(FocusWallpaperManager.serializeWallpapers())
                if (newlyImported.isNotEmpty()) {
                    val first = newlyImported.first()
                    FocusWallpaperManager.setActiveWallpaper(first)
                    viewModel?.setFocusBgCustomUri(first.filePath)
                }
            }
        }
    }

    var showClearWallpapersDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .heightIn(max = 740.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface, // OLED deep forest surface
            tonalElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
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
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Ambient audio, offline music & your backgrounds",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_studio_dialog_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // 5 Dedicated Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 12.dp
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Soundscapes", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Offline Music", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Backgrounds", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.ColorLens, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Online Focus", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Headphones, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Tab(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        text = { Text("Environment", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Tab Content Body
                Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                    when (selectedTab) {
                        0 -> RealSoundscapesTab(
                            currentSoundscape = currentSoundscape,
                            isPlaying = isSoundscapePlaying,
                            volume = soundscapeVolume,
                            onSelect = { ConcentrationSoundscapeHelper.selectSoundscape(it, autoPlay = true) },
                            onTogglePlay = { ConcentrationSoundscapeHelper.togglePlayPause() },
                            onVolumeChange = { ConcentrationSoundscapeHelper.setVolume(it) }
                        )

                        1 -> OfflineMusicPlayerTab(
                            playlist = playlist,
                            currentIndex = currentTrackIndex,
                            currentTrack = currentTrack,
                            isPlaying = isMusicPlaying,
                            positionMs = musicPosMs,
                            durationMs = musicDurationMs,
                            volume = musicVolume,
                            repeatMode = repeatMode,
                            isShuffle = isShuffle,
                            autoPlayWithTimer = autoPlayMusicWithTimer,
                            onAddFiles = { multiAudioPicker.launch(arrayOf("audio/*")) },
                            onSelectTrack = { idx -> StudyMusicPlayer.selectTrack(context, idx, autoPlay = true) },
                            onRemoveTrack = { idx -> StudyMusicPlayer.removeTrack(idx) },
                            onClearPlaylist = { StudyMusicPlayer.clearTrack() },
                            onTogglePlay = { StudyMusicPlayer.togglePlayPause() },
                            onNext = { StudyMusicPlayer.nextTrack() },
                            onPrev = { StudyMusicPlayer.previousTrack() },
                            onSeek = { StudyMusicPlayer.seekTo(it) },
                            onSeekRelative = { StudyMusicPlayer.seekRelative(it) },
                            onVolumeChange = { StudyMusicPlayer.setVolume(it) },
                            onToggleRepeat = { StudyMusicPlayer.toggleRepeatMode() },
                            onToggleShuffle = { StudyMusicPlayer.toggleShuffle() },
                            onToggleAutoPlay = { StudyMusicPlayer.setAutoPlayWithTimer(!autoPlayMusicWithTimer) }
                        )

                        2 -> PersonalBackgroundsTab(
                            bgType = bgType,
                            builtinId = bgBuiltinId,
                            customUri = customUri,
                            wallpapers = importedWallpapers,
                            activeWallpaper = activeWallpaper,
                            overlayOpacity = overlayOpacity,
                            brightness = brightness,
                            isBlur = isBlur,
                            fitMode = fitMode,
                            isSlideshow = isSlideshow,
                            slideshowInterval = slideshowInterval,
                            onSelectOled = {
                                viewModel?.setFocusBgType(FocusBgType.OLED)
                                onThemeSelected(FocusAestheticTheme.MINIMAL_OLED)
                            },
                            onSelectBuiltin = { theme ->
                                viewModel?.setFocusBgBuiltinId(theme.id)
                                onThemeSelected(theme)
                            },
                            onImportPhotos = {
                                multiPhotoPicker.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            onSelectWallpaper = { wallpaper ->
                                FocusWallpaperManager.setActiveWallpaper(wallpaper)
                                viewModel?.setFocusBgCustomUri(wallpaper.filePath)
                            },
                            onDeleteWallpaper = { wallpaper ->
                                coroutineScope.launch {
                                    FocusWallpaperManager.deleteWallpaper(wallpaper)
                                    viewModel?.saveFocusWallpapersList(FocusWallpaperManager.serializeWallpapers())
                                }
                            },
                            onClearAllWallpapers = { showClearWallpapersDialog = true },
                            onOverlayOpacityChange = { viewModel?.setFocusBgOverlayOpacity(it) },
                            onBrightnessChange = { viewModel?.setFocusBgBrightness(it) },
                            onToggleBlur = { viewModel?.setFocusBgBlur(it) },
                            onToggleFitMode = {
                                viewModel?.setFocusBgFit(if (fitMode == "CROP") "FIT" else "CROP")
                            },
                            onToggleSlideshow = { viewModel?.setFocusSlideshowEnabled(it) },
                            onSetSlideshowInterval = { viewModel?.setFocusSlideshowIntervalMinutes(it) }
                        )

                        3 -> OnlineStreamingTab()

                        4 -> EnvironmentAndCuesTab(
                            backgroundAudioAllowed = backgroundAudioAllowed,
                            onToggleBackgroundAudio = { viewModel?.setStudyBackgroundAudioEnabled(it) },
                            immersiveFullscreenEnabled = immersiveFullscreenEnabled,
                            onToggleImmersiveFullscreen = { viewModel?.setStudyImmersiveFullscreenEnabled(it) },
                            keepScreenOn = keepScreenOn,
                            onToggleKeepScreenOn = { viewModel?.setStudyKeepScreenOn(it) },
                            isAmbientAnimation = isAmbientAnimation,
                            onToggleAmbientAnimation = { viewModel?.setFocusAmbientAnimationEnabled(it) },
                            startSoundEnabled = startSoundEnabled,
                            breakStartSoundEnabled = breakStartSoundEnabled,
                            breakEndSoundEnabled = breakEndSoundEnabled,
                            endSoundEnabled = endSoundEnabled,
                            onToggleStartSound = { viewModel?.setStudyStartSoundEnabled(it) },
                            onToggleBreakStartSound = { viewModel?.setStudyBreakStartSoundEnabled(it) },
                            onToggleBreakEndSound = { viewModel?.setStudyBreakEndSoundEnabled(it) },
                            onToggleEndSound = { viewModel?.setStudyEndSoundEnabled(it) },
                            onPreviewCue = { type -> StudySoundHelper.previewSound(context, type) }
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
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("done_studio_button")
                    ) {
                        Text("Apply & Focus", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showClearWallpapersDialog) {
        AlertDialog(
            onDismissRequest = { showClearWallpapersDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            titleContentColor = Color.White,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Remove All Imported Backgrounds?") },
            text = { Text("This will remove all imported images from Rhythm's private offline gallery. Your original device photos will not be deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            FocusWallpaperManager.clearAllWallpapers(context)
                            viewModel?.saveFocusWallpapersList("[]")
                            viewModel?.clearFocusBgCustomUri()
                        }
                        showClearWallpapersDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373), contentColor = Color.Black)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearWallpapersDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

// =========================================================================
// 1. REAL SOUNDSCAPES TAB (10 Environmental Ambient Recordings)
// =========================================================================
@Composable
private fun RealSoundscapesTab(
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
        // Active Controller Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Active Ambient Soundscape", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "${currentSoundscape.iconEmoji} ${currentSoundscape.displayName}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        FilledTonalButton(
                            onClick = onTogglePlay,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isPlaying) "Pause" else "Play", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VolumeDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = volume,
                            onValueChange = onVolumeChange,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Offline generated ambience • These are synthesized sound textures, not real-world field recordings. Authentic rain, forest and water recordings need a separately licensed audio pack.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        items(SoundscapeType.values()) { item ->
            val isSelected = currentSoundscape == item
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(item) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(item.iconEmoji, fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.displayName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        text = item.vibeTag,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (isSelected) {
                        Box(
                            modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 2. OFFLINE MUSIC PLAYER TAB (Multi-track playlist, shuffle, repeat, seek)
// =========================================================================
@Composable
private fun OfflineMusicPlayerTab(
    playlist: List<com.example.util.MusicTrack>,
    currentIndex: Int,
    currentTrack: com.example.util.MusicTrack?,
    isPlaying: Boolean,
    positionMs: Int,
    durationMs: Int,
    volume: Float,
    repeatMode: MusicRepeatMode,
    isShuffle: Boolean,
    autoPlayWithTimer: Boolean,
    onAddFiles: () -> Unit,
    onSelectTrack: (Int) -> Unit,
    onRemoveTrack: (Int) -> Unit,
    onClearPlaylist: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Int) -> Unit,
    onSeekRelative: (Int) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleAutoPlay: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Player Control Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Audiotrack, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Now Playing", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = currentTrack?.title ?: "No Song Selected",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onAddFiles,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Tracks", fontSize = 12.sp)
                        }
                    }

                    if (playlist.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // Scrubber Slider
                        val currentSec = positionMs / 1000
                        val totalSec = durationMs / 1000
                        val posString = String.format(java.util.Locale.getDefault(), "%02d:%02d", currentSec / 60, currentSec % 60)
                        val durString = String.format(java.util.Locale.getDefault(), "%02d:%02d", totalSec / 60, totalSec % 60)

                        Slider(
                            value = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f,
                            onValueChange = { frac -> onSeek((frac * durationMs).toInt()) },
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(posString, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(durString, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Controls: Shuffle, Prev, -10s, Play/Pause, +10s, Next, Repeat
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onToggleShuffle) {
                                Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            IconButton(onClick = onPrev) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous Track", tint = MaterialTheme.colorScheme.onSurface)
                            }

                            IconButton(onClick = { onSeekRelative(-10000) }) {
                                Icon(Icons.Default.Replay10, contentDescription = "Back 10s", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Button(
                                onClick = onTogglePlay,
                                shape = CircleShape,
                                modifier = Modifier.size(52.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                            ) {
                                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
                            }

                            IconButton(onClick = { onSeekRelative(10000) }) {
                                Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            IconButton(onClick = onNext) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next Track", tint = MaterialTheme.colorScheme.onSurface)
                            }

                            IconButton(onClick = onToggleRepeat) {
                                val tint = if (repeatMode != MusicRepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                when (repeatMode) {
                                    MusicRepeatMode.REPEAT_ONE -> Icon(Icons.Default.RepeatOne, contentDescription = "Repeat One", tint = tint)
                                    else -> Icon(Icons.Default.Repeat, contentDescription = "Repeat All", tint = tint)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Volume Control
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Slider(
                                value = volume,
                                onValueChange = onVolumeChange,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                    } else {
                        // Clear Empty State as required by specification
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Add music from your device to create your focus playlist.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Playlist Management Section
        if (playlist.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Focus Playlist (${playlist.size} tracks)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = onClearPlaylist) {
                        Text("Clear All", fontSize = 12.sp, color = Color(0xFFE57373))
                    }
                }
            }

            itemsIndexed(playlist) { index, track ->
                val isCurrent = (index == currentIndex)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isCurrent) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth().clickable { onSelectTrack(index) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${index + 1}",
                                fontSize = 12.sp,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(24.dp)
                            )
                            Icon(
                                if (isCurrent && isPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal),
                                color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(onClick = { onRemoveTrack(index) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Auto-sync setting card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sync with Study Timer", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                        Text("Automatically starts music when study timer starts, and pauses when paused.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = autoPlayWithTimer,
                        onCheckedChange = { onToggleAutoPlay() },
                        colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onPrimary, checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

// =========================================================================
// 3. PERSONAL BACKGROUNDS TAB (OLED + 10 Built-in + My Backgrounds Library)
// =========================================================================
@Composable
private fun PersonalBackgroundsTab(
    bgType: FocusBgType,
    builtinId: String,
    customUri: String?,
    wallpapers: List<FocusWallpaperItem>,
    activeWallpaper: FocusWallpaperItem?,
    overlayOpacity: Float,
    brightness: Float,
    isBlur: Boolean,
    fitMode: String,
    isSlideshow: Boolean,
    slideshowInterval: Int,
    onSelectOled: () -> Unit,
    onSelectBuiltin: (FocusAestheticTheme) -> Unit,
    onImportPhotos: () -> Unit,
    onSelectWallpaper: (FocusWallpaperItem) -> Unit,
    onDeleteWallpaper: (FocusWallpaperItem) -> Unit,
    onClearAllWallpapers: () -> Unit,
    onOverlayOpacityChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onToggleBlur: (Boolean) -> Unit,
    onToggleFitMode: () -> Unit,
    onToggleSlideshow: (Boolean) -> Unit,
    onSetSlideshowInterval: (Int) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Presentation Adjustments Controls
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111713)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2A21)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Presentation & Clarity Controls", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Dark Overlay Opacity (${(overlayOpacity * 100).toInt()}%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        Text("Preserves text legibility", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Slider(
                        value = overlayOpacity,
                        onValueChange = onOverlayOpacityChange,
                        valueRange = 0.20f..0.90f,
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Brightness (${(brightness * 100).toInt()}%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        Icon(Icons.Default.WbSunny, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                    Slider(
                        value = brightness,
                        onValueChange = onBrightnessChange,
                        valueRange = 0.30f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Subtle Soft Blur", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = isBlur,
                            onCheckedChange = onToggleBlur,
                            colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onPrimary, checkedTrackColor = MaterialTheme.colorScheme.primary)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Scale & Crop Mode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            Text("Currently: $fitMode", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(
                            onClick = onToggleFitMode,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (fitMode == "CROP") "Fill / Crop" else "Fit Screen", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // Section A: My Focus Backgrounds Library
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("My Focus Backgrounds", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                            Text("${wallpapers.size} personal wallpapers in offline storage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Button(
                            onClick = onImportPhotos,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Photos", fontSize = 12.sp)
                        }
                    }

                    if (wallpapers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Responsive Grid of Imported Wallpapers
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            wallpapers.chunked(3).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEach { item ->
                                        val isCurrent = (bgType == FocusBgType.CUSTOM && (customUri == item.filePath || activeWallpaper?.id == item.id))
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(95.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(
                                                    width = if (isCurrent) 2.5.dp else 1.dp,
                                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else Color(0xFF233226),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable { onSelectWallpaper(item) }
                                        ) {
                                            AsyncImage(
                                                model = File(item.filePath),
                                                contentDescription = item.displayName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )

                                            // Top-right selection badge or delete action
                                            if (isCurrent) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(6.dp)
                                                        .size(20.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primary)
                                                        .align(Alignment.TopEnd),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(14.dp))
                                                }
                                            }

                                            // Bottom delete icon overlay
                                            IconButton(
                                                onClick = { onDeleteWallpaper(item) },
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .align(Alignment.BottomEnd)
                                                    .padding(2.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                    // Fill empty slots in last row if needed
                                    val emptySlots = 3 - rowItems.size
                                    for (i in 0 until emptySlots) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Slideshow Mode & Clear All
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = isSlideshow,
                                    onCheckedChange = onToggleSlideshow,
                                    colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onPrimary, checkedTrackColor = MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Slideshow (${slideshowInterval}m)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            }

                            TextButton(onClick = onClearAllWallpapers) {
                                Text("Clear Gallery", fontSize = 11.sp, color = Color(0xFFE57373))
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Import multiple wallpapers to build your persistent offline focus gallery.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Section B: Pure OLED Black
        item {
            val isOledSelected = bgType == FocusBgType.OLED || (bgType == FocusBgType.BUILTIN && builtinId == "minimal_oled")
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isOledSelected) 2.dp else 1.dp,
                    color = if (isOledSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background
                ),
                modifier = Modifier.fillMaxWidth().clickable { onSelectOled() }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Pure Minimal OLED Black", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                        Text("Zero distraction true black, maximum battery saving on OLED displays", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (isOledSelected) {
                        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Section C: 10 Curated Atmospheric Backdrops Header
        item {
            Text(
                text = "Curated Atmospheric Backdrops",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(FocusAestheticTheme.values()) { theme ->
            if (theme == FocusAestheticTheme.MINIMAL_OLED) return@items // Handled above

            val isSelected = (bgType == FocusBgType.BUILTIN && builtinId.equals(theme.id, ignoreCase = true))
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = theme.cardBackground),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) theme.primaryAccent else Color.White.copy(alpha = 0.10f)
                ),
                modifier = Modifier.fillMaxWidth().clickable { onSelectBuiltin(theme) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(theme.primaryAccent))
                            Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(theme.secondaryAccent))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(theme.displayName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                            Text(theme.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (isSelected) {
                        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(theme.primaryAccent), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 4. ONLINE FOCUS & STREAMING TAB (Official External App Integrations)
// =========================================================================
@Composable
private fun OnlineStreamingTab() {
    val context = LocalContext.current

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Official Streaming Music Integrations", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Rhythm strictly complies with open-source licensing and streaming terms of service. External music runs through your official installed applications.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Spotify
        item {
            OnlineServiceCard(
                title = "Spotify Focus Playlists",
                description = "Open Deep Focus, Lofi Beats, or Ambient Study in the official Spotify app",
                buttonText = "Open in Spotify",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/genre/focus"))
                    context.startActivity(intent)
                }
            )
        }

        // YouTube Lofi & Study
        item {
            OnlineServiceCard(
                title = "YouTube Focus & Lofi",
                description = "Launch curated Lofi Girl and atmospheric study livestreams in the YouTube app",
                buttonText = "Open in YouTube",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=lofi+focus+study"))
                    context.startActivity(intent)
                }
            )
        }

        // Amazon Music
        item {
            OnlineServiceCard(
                title = "Amazon Music Focus",
                description = "Launch instrumental and peaceful ambient playlists in Amazon Music",
                buttonText = "Open in Amazon Music",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://music.amazon.com"))
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
private fun OnlineServiceCard(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(10.dp))
            OutlinedButton(
                onClick = onClick,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(buttonText, fontSize = 11.sp)
            }
        }
    }
}

// =========================================================================
// 5. ENVIRONMENT & AUDIO CUES TAB
// =========================================================================
@Composable
private fun EnvironmentAndCuesTab(
    backgroundAudioAllowed: Boolean,
    onToggleBackgroundAudio: (Boolean) -> Unit,
    immersiveFullscreenEnabled: Boolean,
    onToggleImmersiveFullscreen: (Boolean) -> Unit,
    keepScreenOn: Boolean,
    onToggleKeepScreenOn: (Boolean) -> Unit,
    isAmbientAnimation: Boolean,
    onToggleAmbientAnimation: (Boolean) -> Unit,
    startSoundEnabled: Boolean,
    breakStartSoundEnabled: Boolean,
    breakEndSoundEnabled: Boolean,
    endSoundEnabled: Boolean,
    onToggleStartSound: (Boolean) -> Unit,
    onToggleBreakStartSound: (Boolean) -> Unit,
    onToggleBreakEndSound: (Boolean) -> Unit,
    onToggleEndSound: (Boolean) -> Unit,
    onPreviewCue: (StudySoundType) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("Focus Environment & Lifecycle", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        }

        // P0 Feature: Audio while studying in other apps
        item {
            EnvironmentSwitchCard(
                title = "Audio while studying in other apps",
                description = "Off by default. When enabled, focus audio may continue in background while timer runs. Pausing always stops audio.",
                checked = backgroundAudioAllowed,
                onCheckedChange = onToggleBackgroundAudio
            )
        }

        // P0 Feature: Immersive Full-Screen Timer
        item {
            EnvironmentSwitchCard(
                title = "Immersive Full-Screen Timer",
                description = "Hides Android status bar and navigation bars when entering focus mode for true distraction-free fullscreen.",
                checked = immersiveFullscreenEnabled,
                onCheckedChange = onToggleImmersiveFullscreen
            )
        }

        item {
            EnvironmentSwitchCard(
                title = "Keep Screen Awake",
                description = "Prevents device display from sleeping while a focus sprint or break is actively running.",
                checked = keepScreenOn,
                onCheckedChange = onToggleKeepScreenOn
            )
        }

        item {
            EnvironmentSwitchCard(
                title = "Ambient Breathing Motion",
                description = "Slow, rhythmic pulse in ambient background orbs. Disable for completely static stillness.",
                checked = isAmbientAnimation,
                onCheckedChange = onToggleAmbientAnimation
            )
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text("Gentle Audio Signals", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        }

        item {
            CueCard(title = "Session Start Bell", description = "Deep gong when study sprint begins", isChecked = startSoundEnabled, onToggle = onToggleStartSound, onPreview = { onPreviewCue(StudySoundType.SESSION_START) })
        }
        item {
            CueCard(title = "Break Start Chime", description = "Ascending chime when break begins", isChecked = breakStartSoundEnabled, onToggle = onToggleBreakStartSound, onPreview = { onPreviewCue(StudySoundType.BREAK_START) })
        }
        item {
            CueCard(title = "Break Ending Notice", description = "30s warning before break concludes", isChecked = breakEndSoundEnabled, onToggle = onToggleBreakEndSound, onPreview = { onPreviewCue(StudySoundType.BREAK_END) })
        }
        item {
            CueCard(title = "Session Complete Bell", description = "Warm celebratory chord at target duration", isChecked = endSoundEnabled, onToggle = onToggleEndSound, onPreview = { onPreviewCue(StudySoundType.SESSION_END) })
        }
    }
}

@Composable
private fun EnvironmentSwitchCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onPrimary, checkedTrackColor = MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun CueCard(
    title: String,
    description: String,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit,
    onPreview: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onPreview,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isChecked,
                    onCheckedChange = onToggle,
                    colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onPrimary, checkedTrackColor = MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
