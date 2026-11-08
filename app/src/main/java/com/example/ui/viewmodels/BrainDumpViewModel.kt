package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.BrainDumpNoteEntity
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BrainDumpViewModel(
    private val repository: AppRepository
) : ViewModel() {

    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage: StateFlow<String?> = _userFeedbackMessage.asStateFlow()

    private val _brainDumpSearchQuery = MutableStateFlow("")
    val brainDumpSearchQuery: StateFlow<String> = _brainDumpSearchQuery.asStateFlow()

    private val _brainDumpTagFilter = MutableStateFlow("All")
    val brainDumpTagFilter: StateFlow<String> = _brainDumpTagFilter.asStateFlow()

    val allBrainDumpNotes: StateFlow<List<BrainDumpNoteEntity>> = repository.allBrainDumpNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeBrainDumpNotes: StateFlow<List<BrainDumpNoteEntity>> = combine(
        repository.activeBrainDumpNotes,
        _brainDumpSearchQuery,
        _brainDumpTagFilter
    ) { notes, query, tag ->
        notes.filter { note ->
            val matchesQuery = query.isBlank() ||
                (note.title?.contains(query, ignoreCase = true) == true) ||
                note.content.contains(query, ignoreCase = true)
            val matchesTag = tag == "All" || note.tag.equals(tag, ignoreCase = true)
            matchesQuery && matchesTag
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedBrainDumpNotes: StateFlow<List<BrainDumpNoteEntity>> = repository.archivedBrainDumpNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearFeedbackMessage() {
        _userFeedbackMessage.value = null
    }

    fun setBrainDumpSearchQuery(query: String) {
        _brainDumpSearchQuery.value = query
    }

    fun setBrainDumpTagFilter(tag: String) {
        _brainDumpTagFilter.value = tag
    }

    fun saveBrainDumpNote(
        id: Long? = null,
        title: String?,
        content: String,
        tag: String = "General",
        isPinned: Boolean = false
    ) {
        val trimmedContent = content.trim()
        if (trimmedContent.isBlank()) {
            _userFeedbackMessage.value = "Note content cannot be empty"
            return
        }

        viewModelScope.launch {
            if (id != null && id > 0L) {
                val existing = repository.getBrainDumpNoteById(id)
                if (existing != null) {
                    repository.updateBrainDumpNote(
                        existing.copy(
                            title = title?.trim()?.ifBlank { null },
                            content = trimmedContent,
                            tag = tag.trim().ifBlank { "General" },
                            isPinned = isPinned,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    _userFeedbackMessage.value = "Thought updated"
                    return@launch
                }
            }

            repository.saveBrainDumpNote(
                BrainDumpNoteEntity(
                    title = title?.trim()?.ifBlank { null },
                    content = trimmedContent,
                    tag = tag.trim().ifBlank { "General" },
                    isPinned = isPinned,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            _userFeedbackMessage.value = "Thought captured"
        }
    }

    suspend fun saveBrainDumpNoteSync(
        title: String?,
        content: String,
        tag: String = "General",
        isPinned: Boolean = false
    ): Long {
        return repository.saveBrainDumpNote(
            BrainDumpNoteEntity(
                title = title?.trim()?.ifBlank { null },
                content = content.trim(),
                tag = tag.trim().ifBlank { "General" },
                isPinned = isPinned,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    fun deleteBrainDumpNote(id: Long) {
        viewModelScope.launch {
            repository.deleteBrainDumpNote(id)
            _userFeedbackMessage.value = "Thought deleted"
        }
    }

    fun togglePinBrainDumpNote(note: BrainDumpNoteEntity) {
        viewModelScope.launch {
            val newPinned = !note.isPinned
            repository.setBrainDumpNotePinned(note.id, newPinned)
            _userFeedbackMessage.value = if (newPinned) "Thought pinned to top" else "Thought unpinned"
        }
    }

    fun toggleArchiveBrainDumpNote(note: BrainDumpNoteEntity) {
        viewModelScope.launch {
            val newArchived = !note.isArchived
            repository.setBrainDumpNoteArchived(note.id, newArchived)
            _userFeedbackMessage.value = if (newArchived) "Thought archived" else "Thought unarchived"
        }
    }

    fun convertBrainDumpNoteToStudyTask(
        note: BrainDumpNoteEntity,
        category: String = "General",
        estimatedMinutes: Int = 25,
        isPriority: Boolean = false
    ) {
        viewModelScope.launch {
            convertBrainDumpNoteToStudyTaskSync(note, category, estimatedMinutes, isPriority)
        }
    }

    suspend fun convertBrainDumpNoteToStudyTaskSync(
        note: BrainDumpNoteEntity,
        category: String = "General",
        estimatedMinutes: Int = 25,
        isPriority: Boolean = false
    ): Long {
        val taskId = repository.convertNoteToStudyTask(
            note = note,
            category = category,
            estimatedMinutes = estimatedMinutes
        )
        if (isPriority) {
            repository.setDailyPriority(taskId)
        }
        _userFeedbackMessage.value = "Converted to study task in '$category'"
        return taskId
    }
}

