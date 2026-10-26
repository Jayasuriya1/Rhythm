package com.example.ui.screens.braindump

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.BrainDumpNoteEntity
import com.example.ui.MainViewModel
import com.example.ui.components.CalmEmptyState

@Composable
fun BrainDumpScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeNotes by viewModel.activeBrainDumpNotes.collectAsStateWithLifecycle()
    val archivedNotes by viewModel.archivedBrainDumpNotes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.brainDumpSearchQuery.collectAsStateWithLifecycle()
    val selectedTagFilter by viewModel.brainDumpTagFilter.collectAsStateWithLifecycle()
    val activeCategories by viewModel.activeCategories.collectAsStateWithLifecycle()

    var showQuickCaptureDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<BrainDumpNoteEntity?>(null) }
    var noteToConvert by remember { mutableStateOf<BrainDumpNoteEntity?>(null) }
    var showArchivedTab by remember { mutableStateOf(false) }

    val currentNotes = if (showArchivedTab) archivedNotes else activeNotes

    val filteredNotes = currentNotes.filter { note ->
        val matchesQuery = searchQuery.isBlank() ||
            (note.title?.contains(searchQuery, ignoreCase = true) == true) ||
            note.content.contains(searchQuery, ignoreCase = true) ||
            (note.tag?.contains(searchQuery, ignoreCase = true) == true)

        val matchesTag = selectedTagFilter == "All" || note.tag.equals(selectedTagFilter, ignoreCase = true)

        matchesQuery && matchesTag
    }

    val pinnedNotes = filteredNotes.filter { it.isPinned }
    val unpinnedNotes = filteredNotes.filter { !it.isPinned }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showQuickCaptureDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_quick_thought_capture")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Capture Thought")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Capture Thought")
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Description Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Brain Dump & Thought Parking",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Quickly dump intrusive thoughts, project ideas, or study distractions so you can stay in flow.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showQuickCaptureDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("add_thought_header_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Thought")
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setBrainDumpSearchQuery(it) },
                    placeholder = { Text("Search thoughts or keywords...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setBrainDumpSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("brain_dump_search_field")
                )
            }

            // Tags and Tab Selectors
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        val tags = listOf("All") + BRAIN_DUMP_TAGS
                        items(tags) { tag ->
                            FilterChip(
                                selected = selectedTagFilter == tag,
                                onClick = { viewModel.setBrainDumpTagFilter(tag) },
                                label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    TextButton(
                        onClick = { showArchivedTab = !showArchivedTab },
                        modifier = Modifier.testTag("toggle_archive_view_button")
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showArchivedTab) "Active" else "Archive", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Pinned Notes Section
            if (pinnedNotes.isNotEmpty() && !showArchivedTab) {
                item {
                    Text(
                        text = "Pinned Thoughts (${pinnedNotes.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(pinnedNotes, key = { it.id }) { note ->
                    BrainDumpNoteItemCard(
                        note = note,
                        onEdit = { noteToEdit = note },
                        onTogglePin = { viewModel.togglePinBrainDumpNote(note) },
                        onToggleArchive = { viewModel.toggleArchiveBrainDumpNote(note) },
                        onConvertToTask = { noteToConvert = note },
                        onDelete = { viewModel.deleteBrainDumpNote(note.id) }
                    )
                }
            }

            // Recent / Other Notes Section
            if (unpinnedNotes.isNotEmpty()) {
                if (pinnedNotes.isNotEmpty() && !showArchivedTab) {
                    item {
                        Text(
                            text = "Recent Thoughts",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }

                items(unpinnedNotes, key = { it.id }) { note ->
                    BrainDumpNoteItemCard(
                        note = note,
                        onEdit = { noteToEdit = note },
                        onTogglePin = { viewModel.togglePinBrainDumpNote(note) },
                        onToggleArchive = { viewModel.toggleArchiveBrainDumpNote(note) },
                        onConvertToTask = { noteToConvert = note },
                        onDelete = { viewModel.deleteBrainDumpNote(note.id) }
                    )
                }
            }

            // Empty State
            if (filteredNotes.isEmpty()) {
                item {
                    CalmEmptyState(
                        icon = Icons.Default.Lightbulb,
                        title = if (showArchivedTab) "No Archived Thoughts" else "No Thoughts Captured Yet",
                        message = if (showArchivedTab) "Thoughts you archive will be stored here." else "Jot down sudden thoughts or distracting study questions so your mind can rest.",
                        actionLabel = if (!showArchivedTab) "Capture First Thought" else null,
                        onActionClick = { showQuickCaptureDialog = true }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    // Quick Capture / Edit Dialog
    if (showQuickCaptureDialog || noteToEdit != null) {
        QuickCaptureDialog(
            noteToEdit = noteToEdit,
            onDismiss = {
                showQuickCaptureDialog = false
                noteToEdit = null
            },
            onSave = { id, title, content, tag, isPinned ->
                viewModel.saveBrainDumpNote(id, title, content, tag, isPinned)
            },
            onConvertToTaskDirectly = { taskTitle, tag, _ ->
                viewModel.createStudyTask(taskTitle, if (tag == "Study") "General" else tag, 25, false)
            }
        )
    }

    // Convert to Task Dialog
    noteToConvert?.let { note ->
        ConvertNoteToTaskDialog(
            note = note,
            activeCategories = activeCategories,
            onDismiss = { noteToConvert = null },
            onConfirm = { category, minutes, isPriority ->
                viewModel.convertBrainDumpNoteToStudyTask(note, category, minutes, isPriority)
                noteToConvert = null
            }
        )
    }
}
