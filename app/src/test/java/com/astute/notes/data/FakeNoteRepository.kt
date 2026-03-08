package com.astute.notes.data

import com.astute.notes.model.Note
import java.util.UUID

class FakeNoteRepository : NoteRepository {

    private val notes = mutableMapOf<String, Note>()
    var shouldThrow: Boolean = false

    override suspend fun listNotes(): List<Note> {
        if (shouldThrow) throw RuntimeException("Fake error")
        return notes.values.sortedByDescending { it.updatedAt }
    }

    override suspend fun getNoteById(id: String): Note? {
        if (shouldThrow) throw RuntimeException("Fake error")
        return notes[id]
    }

    override suspend fun createNote(title: String, body: String): Note {
        if (shouldThrow) throw RuntimeException("Fake error")
        val now = System.currentTimeMillis()
        val note = Note(
            id = UUID.randomUUID().toString(),
            title = title,
            body = body,
            createdAt = now,
            updatedAt = now
        )
        notes[note.id] = note
        return note
    }

    override suspend fun updateNote(note: Note): Note {
        if (shouldThrow) throw RuntimeException("Fake error")
        val updated = note.copy(updatedAt = System.currentTimeMillis())
        notes[updated.id] = updated
        return updated
    }

    override suspend fun saveNote(note: Note) {
        if (shouldThrow) throw RuntimeException("Fake error")
        notes[note.id] = note
    }

    override suspend fun deleteNote(id: String) {
        if (shouldThrow) throw RuntimeException("Fake error")
        notes.remove(id)
    }

    fun seed(vararg seedNotes: Note) {
        seedNotes.forEach { notes[it.id] = it }
    }

    fun allNotes(): List<Note> = notes.values.toList()
}
