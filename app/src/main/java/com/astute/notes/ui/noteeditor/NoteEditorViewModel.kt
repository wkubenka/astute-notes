package com.astute.notes.ui.noteeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astute.notes.AstuteNotesApp
import com.astute.notes.data.NoteRepository
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
    private val repository: NoteRepository = AstuteNotesApp.instance.repository
) : ViewModel() {

    private var existingNote: com.astute.notes.model.Note? = null

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
                val existing = existingNote
                if (existing != null) {
                    repository.updateNote(
                        existing.copy(
                            title = _uiState.value.title,
                            body = _uiState.value.body
                        )
                    )
                } else {
                    repository.createNote(
                        title = _uiState.value.title,
                        body = _uiState.value.body
                    )
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
}
