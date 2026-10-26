package com.example.ui.screens.braindump

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StarBorder
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.BrainDumpNoteEntity
import com.example.data.entity.StudyCategoryEntity
import com.example.util.DateTimeUtils

val BRAIN_DUMP_TAGS = listOf("Idea", "Study", "Todo", "Health", "Personal", "Distraction")

@Composable
fun QuickCaptureDialog(
    noteToEdit: BrainDumpNoteEntity? = null,
    onDismiss: () -> Unit,
    onSave: (Long?, String?, String, String, Boolean) -> Unit,
    onConvertToTaskDirectly: ((String, String, String) -> Unit)? = null
) {
    val context = LocalContext.current
    var content by remember(noteToEdit) { mutableStateOf(noteToEdit?.content ?: "") }
    var title by remember(noteToEdit) { mutableStateOf(noteToEdit?.title ?: "") }
    var selectedTag by remember(noteToEdit) { mutableStateOf(noteToEdit?.tag ?: "Idea") }
    var isPinned by remember(noteToEdit) { mutableStateOf(noteToEdit?.isPinned ?: false) }
    var showDiscardConfirmation by remember { mutableStateOf(false) }
    var showTitleField by remember { mutableStateOf(!noteToEdit?.title.isNullOrBlank()) }

    val hasUnsavedChanges = content.isNotBlank() && (noteToEdit == null || content != noteToEdit.content || title != (noteToEdit.title ?: ""))

    AlertDialog(
        onDismissRequest = {
            if (hasUnsavedChanges) {
                showDiscardConfirmation = true
            } else {
                onDismiss()
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (noteToEdit != null) "Edit Thought" else "Quick Brain Dump",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (content.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = ClipData.newPlainText("Thought", content)
                                clipboard?.setPrimaryClip(clip)
                                Toast.makeText(context, "Thought copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp).testTag("quick_capture_copy_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy thought",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = { isPinned = !isPinned },
                        modifier = Modifier.size(36.dp).testTag("quick_capture_pin_button")
                    ) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin to top",
                            tint = if (isPinned) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (showTitleField) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title (Optional)") },
                        placeholder = { Text("e.g. Memory retention idea") },
                        modifier = Modifier.fillMaxWidth().testTag("brain_dump_title_input")
                    )
                } else {
                    TextButton(
                        onClick = { showTitleField = true },
                        modifier = Modifier.align(Alignment.End).testTag("add_custom_title_button")
                    ) {
                        Text("+ Add Custom Title", style = MaterialTheme.typography.labelSmall)
                    }
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("What's on your mind? *") },
                    placeholder = { Text("Jot down quick thoughts, distractions to park, study insights, or tasks...") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth().testTag("brain_dump_content_input")
                )

                Text("Tag", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(BRAIN_DUMP_TAGS) { tag ->
                        FilterChip(
                            selected = selectedTag == tag,
                            onClick = { selectedTag = tag },
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("tag_chip_${tag.lowercase()}")
                        )
                    }
                }

                if (onConvertToTaskDirectly != null && content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = {
                            val finalTitle = title.ifBlank {
                                val firstLine = content.lineSequence().firstOrNull()?.trim() ?: "Study Task"
                                if (firstLine.length > 50) firstLine.take(47) + "..." else firstLine
                            }
                            onConvertToTaskDirectly(finalTitle, selectedTag, content)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("convert_direct_task_button")
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Convert to Study Task Directly", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (content.isNotBlank()) {
                        onSave(noteToEdit?.id, title, content, selectedTag, isPinned)
                        onDismiss()
                    }
                },
                enabled = content.isNotBlank(),
                modifier = Modifier.testTag("save_brain_dump_button")
            ) {
                Text(if (noteToEdit != null) "Update" else "Save Thought")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (hasUnsavedChanges) {
                        showDiscardConfirmation = true
                    } else {
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("cancel_brain_dump_button")
            ) {
                Text("Cancel")
            }
        }
    )

    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text("Discard Unsaved Thought?") },
            text = { Text("You have typed a thought. Discarding will lose these words.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardConfirmation = false
                        onDismiss()
                    }
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmation = false }) {
                    Text("Keep Editing")
                }
            }
        )
    }
}

@Composable
fun ConvertNoteToTaskDialog(
    note: BrainDumpNoteEntity,
    activeCategories: List<StudyCategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Boolean) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(activeCategories.firstOrNull()?.name ?: "General") }
    var estimatedMinutes by remember { mutableIntStateOf(25) }
    var isPriority by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Convert Thought to Study Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = note.displayTitle(),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = note.content,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3
                        )
                    }
                }

                Text("Target Study Category", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(activeCategories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat.name,
                            onClick = { selectedCategory = cat.name },
                            label = { Text(cat.name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Text("Estimated Duration: $estimatedMinutes mins", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 25, 45, 60).forEach { mins ->
                        FilterChip(
                            selected = estimatedMinutes == mins,
                            onClick = { estimatedMinutes = mins },
                            label = { Text("${mins}m") }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable { isPriority = !isPriority }
                ) {
                    Icon(
                        imageVector = if (isPriority) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = null,
                        tint = if (isPriority) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                    )
                    Text("Set as Today's Main Priority", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedCategory, estimatedMinutes, isPriority) },
                modifier = Modifier.testTag("confirm_convert_task_button")
            ) {
                Text("Convert to Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun BrainDumpNoteItemCard(
    note: BrainDumpNoteEntity,
    onEdit: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleArchive: () -> Unit,
    onConvertToTask: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            },
            title = {
                Text(
                    text = "Delete Thought?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete this thought? Stored text will be removed from your database.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_note_btn")
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false },
                    modifier = Modifier.testTag("cancel_delete_note_btn")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (note.isPinned) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        ),
        modifier = modifier.fillMaxWidth().testTag("brain_dump_item_${note.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = note.tag ?: "General",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (note.isPinned) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Filled.PushPin, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                Text("Pinned", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                        }
                    }

                    Text(
                        text = DateTimeUtils.formatTimestamp(note.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clip = ClipData.newPlainText("Thought", note.content)
                            clipboard?.setPrimaryClip(clip)
                            Toast.makeText(context, "Thought copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp).testTag("copy_note_${note.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Thought",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.size(32.dp).testTag("pin_note_${note.id}")
                    ) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin",
                            modifier = Modifier.size(16.dp),
                            tint = if (note.isPinned) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp).testTag("edit_note_${note.id}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!note.title.isNullOrBlank()) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action row: Convert to task, Archive/Unarchive, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onConvertToTask,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("convert_task_${note.id}")
                ) {
                    Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("To Study Task", style = MaterialTheme.typography.labelSmall)
                }

                FilledTonalButton(
                    onClick = onToggleArchive,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("archive_note_${note.id}")
                ) {
                    Icon(
                        imageVector = if (note.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (note.isArchived) "Restore" else "Archive", style = MaterialTheme.typography.labelSmall)
                }

                IconButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.size(32.dp).testTag("delete_note_${note.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}
