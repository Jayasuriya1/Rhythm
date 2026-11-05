package com.example.ui.screens.study

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.util.ConcentrationSoundscapeHelper
import com.example.util.SoundscapeType
import com.example.util.StudyMusicPlayer

/**
 * Modern Focus Audio & Aesthetic Studio Dialog inspired by Flocus and Wonderspace.
 * Offers 3 distinct tabs:
 * 1. Focus Soundscapes (Brown noise, Rain, Forest, Cafe, Waves, White noise)
 * 2. Local Music / Song Player (Pick any local music file on phone, seek, volume, auto-sync)
 * 3. Aesthetic Backdrop Themes (Lofi Cafe, Midnight Cyber, Zen Forest, Deep Space, Sunset, Minimal)
 */
@Composable
fun FocusBackdropAudioStudioDialog(
    currentTheme: FocusAestheticTheme,
    onThemeSelected: (FocusAestheticTheme) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    // Soundscape State
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

    // SAF File Picker for local audio
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            StudyMusicPlayer.loadLocalTrack(context, it)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
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
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Aesthetic Themes & Concentration Audio",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_studio_dialog_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Tab Selector
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Soundscapes", style = MaterialTheme.typography.labelMedium) },
                        icon = { Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("My Music", style = MaterialTheme.typography.labelMedium) },
                        icon = { Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Themes", style = MaterialTheme.typography.labelMedium) },
                        icon = { Icon(Icons.Default.ColorLens, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                // Tab Content
                Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                    when (selectedTab) {
                        0 -> SoundscapesTabContent(
                            currentSoundscape = currentSoundscape,
                            isPlaying = isSoundscapePlaying,
                            volume = soundscapeVolume,
                            onSelect = { ConcentrationSoundscapeHelper.selectSoundscape(it, autoPlay = true) },
                            onTogglePlay = { ConcentrationSoundscapeHelper.togglePlayPause() },
                            onVolumeChange = { ConcentrationSoundscapeHelper.setVolume(it) }
                        )

                        1 -> LocalMusicTabContent(
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

                        2 -> AestheticThemesTabContent(
                            currentTheme = currentTheme,
                            onSelectTheme = onThemeSelected
                        )
                    }
                }

                // Bottom Footer Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("done_studio_button")
                    ) {
                        Text("Apply & Focus")
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundscapesTabContent(
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
        // Master Audio Control Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
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
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${currentSoundscape.iconEmoji} ${currentSoundscape.displayName}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        FilledTonalButton(
                            onClick = onTogglePlay,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isPlaying) "Pause" else "Play")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.VolumeDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = volume,
                            onValueChange = onVolumeChange,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(item) }
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp)
                    )
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
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = item.vibeTag,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Active",
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

@Composable
private fun LocalMusicTabContent(
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
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Audiotrack,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Device Music / Song",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = trackTitle ?: "No Song Selected",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onPickFile,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select File")
                        }
                    }

                    if (hasTrack) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Audio Scrubber
                        val currentSec = positionMs / 1000
                        val totalSec = durationMs / 1000
                        val posString = String.format(java.util.Locale.getDefault(), "%02d:%02d", currentSec / 60, currentSec % 60)
                        val durString = String.format(java.util.Locale.getDefault(), "%02d:%02d", totalSec / 60, totalSec % 60)

                        Slider(
                            value = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f,
                            onValueChange = { frac ->
                                onSeek((frac * durationMs).toInt())
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = posString, style = MaterialTheme.typography.labelSmall)
                            Text(text = durString, style = MaterialTheme.typography.labelSmall)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Playback Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onToggleLoop) {
                                Icon(
                                    Icons.Default.Repeat,
                                    contentDescription = "Loop Song",
                                    tint = if (isLooping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = onTogglePlay,
                                shape = CircleShape,
                                modifier = Modifier.size(54.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            OutlinedButton(
                                onClick = onClearTrack,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Clear", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Volume Control
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.VolumeDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Slider(
                                value = volume,
                                onValueChange = onVolumeChange,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tap 'Select File' to choose any MP3, M4A, or WAV file from your phone storage to play alongside your study timer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Auto-play Toggle Setting
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(14.dp)
                    )
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
                            text = "Sync with Study Timer",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Automatically start music when timer starts, and pause when timer is paused.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = autoPlayWithTimer,
                        onCheckedChange = { onToggleAutoPlay() }
                    )
                }
            }
        }
    }
}

@Composable
private fun AestheticThemesTabContent(
    currentTheme: FocusAestheticTheme,
    onSelectTheme: (FocusAestheticTheme) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(FocusAestheticTheme.values()) { theme ->
            val isSelected = currentTheme == theme
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = theme.cardBackground
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectTheme(theme) }
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) theme.primaryAccent else Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        // Theme Palette Circles
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
                                color = Color.White.copy(alpha = 0.7f)
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
