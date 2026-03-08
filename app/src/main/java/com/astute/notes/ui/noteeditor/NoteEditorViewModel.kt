package com.astute.notes.ui.noteeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astute.notes.AstuteNotesApp
import com.astute.notes.data.NoteRepository
import com.astute.notes.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NoteEditorUiState(
    val title: String = "",
    val body: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false
)

class NoteEditorViewModel(
    private val repository: NoteRepository = AstuteNotesApp.instance.repository,
    private val onBackup: (suspend (Note) -> Unit)? = defaultBackup()
) : ViewModel() {

    private var existingNote: Note? = null

    private val _uiState = MutableStateFlow(NoteEditorUiState())
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    val isNewNote: Boolean get() = existingNote == null

    fun loadNote(noteId: String?) {
        if (noteId == null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val note = repository.getNoteById(noteId)
                if (note != null) {
                    existingNote = note
                    _uiState.value = _uiState.value.copy(
                        title = note.title,
                        body = note.body,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Note not found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load note"
                )
            }
        }
    }

    fun onTitleChanged(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun onBodyChanged(body: String) {
        _uiState.value = _uiState.value.copy(body = body)
    }

    fun saveNote() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            try {
                val saved: Note
                val existing = existingNote
                if (existing != null) {
                    saved = repository.updateNote(
                        existing.copy(
                            title = _uiState.value.title,
                            body = _uiState.value.body
                        )
                    )
                } else {
                    saved = repository.createNote(
                        title = _uiState.value.title,
                        body = _uiState.value.body
                    )
                }

                // Best-effort S3 backup
                try {
                    onBackup?.invoke(saved)
                } catch (_: Exception) {
                    // Backup failure is non-fatal
                }

                _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: "Failed to save note"
                )
            }
        }
    }

    companion object {
        private fun defaultBackup(): (suspend (Note) -> Unit)? {
            return try {
                val app = AstuteNotesApp.instance
                val s3Repo = app.createS3Repository() ?: return null
                { note -> s3Repo.backupNotes(listOf(note)) }
            } catch (_: Exception) {
                null
            }
        }
    }
}
