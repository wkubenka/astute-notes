package com.astutenotes.ui.notelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astutenotes.AstuteNotesApp
import com.astutenotes.data.NoteRepository
import com.astutenotes.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NoteListUiState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class NoteListViewModel(
    private val repository: NoteRepository = AstuteNotesApp.instance.repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteListUiState())
    val uiState: StateFlow<NoteListUiState> = _uiState.asStateFlow()

    init {
        loadNotes()
    }

    fun loadNotes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val notes = repository.listNotes()
                _uiState.value = _uiState.value.copy(notes = notes, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load notes"
                )
            }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            try {
                repository.deleteNote(noteId)
                loadNotes()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to delete note"
                )
            }
        }
    }
}
