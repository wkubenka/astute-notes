package com.astute.notes.data

import com.astute.notes.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class LocalNoteRepository(private val notesDir: File) : NoteRepository {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    init {
        notesDir.mkdirs()
    }

    override suspend fun listNotes(): List<Note> = withContext(Dispatchers.IO) {
        notesDir.listFiles { file -> file.extension == "json" }
            ?.mapNotNull { file ->
                try {
                    json.decodeFromString<Note>(file.readText())
                } catch (_: Exception) {
                    null
                }
            }
            ?.sortedByDescending { it.updatedAt }
            ?: emptyList()
    }

    override suspend fun getNoteById(id: String): Note? = withContext(Dispatchers.IO) {
        val file = File(notesDir, "$id.json")
        if (!file.exists()) return@withContext null
        try {
            json.decodeFromString<Note>(file.readText())
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun createNote(title: String, body: String): Note {
        val now = System.currentTimeMillis()
        val note = Note(
            id = UUID.randomUUID().toString(),
            title = title,
            body = body,
            createdAt = now,
            updatedAt = now
        )
        saveNote(note)
        return note
    }

    override suspend fun updateNote(note: Note): Note {
        val updated = note.copy(updatedAt = System.currentTimeMillis())
        saveNote(updated)
        return updated
    }

    override suspend fun deleteNote(id: String) = withContext(Dispatchers.IO) {
        File(notesDir, "$id.json").delete()
        Unit
    }

    private suspend fun saveNote(note: Note) = withContext(Dispatchers.IO) {
        val file = File(notesDir, "${note.id}.json")
        file.writeText(json.encodeToString(note))
    }
}
