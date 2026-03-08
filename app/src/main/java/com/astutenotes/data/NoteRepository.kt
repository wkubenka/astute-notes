package com.astutenotes.data

import com.astutenotes.model.Note

interface NoteRepository {
    suspend fun listNotes(): List<Note>
    suspend fun getNoteById(id: String): Note?
    suspend fun createNote(title: String, body: String): Note
    suspend fun updateNote(note: Note): Note
    suspend fun deleteNote(id: String)
}
