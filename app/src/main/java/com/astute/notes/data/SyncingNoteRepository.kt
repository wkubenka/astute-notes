package com.astute.notes.data

import com.astute.notes.model.Note

class SyncingNoteRepository(
    private val local: NoteRepository
) : NoteRepository {

    var remote: NoteRepository? = null

    override suspend fun listNotes(): List<Note> = local.listNotes()

    override suspend fun getNoteById(id: String): Note? = local.getNoteById(id)

    override suspend fun createNote(title: String, body: String): Note {
        val note = local.createNote(title, body)
        try { remote?.saveNote(note) } catch (_: Exception) {}
        return note
    }

    override suspend fun updateNote(note: Note): Note {
        val updated = local.updateNote(note)
        try { remote?.saveNote(updated) } catch (_: Exception) {}
        return updated
    }

    override suspend fun saveNote(note: Note) {
        local.saveNote(note)
        try { remote?.saveNote(note) } catch (_: Exception) {}
    }

    override suspend fun deleteNote(id: String) {
        local.deleteNote(id)
        try { remote?.deleteNote(id) } catch (_: Exception) {}
    }

    suspend fun sync() {
        val s3 = remote ?: return

        val localNotes = local.listNotes()
        val remoteNotes = s3.listNotes()

        val localMap = localNotes.associateBy { it.id }
        val remoteMap = remoteNotes.associateBy { it.id }

        val allIds = localMap.keys + remoteMap.keys

        for (id in allIds) {
            val localNote = localMap[id]
            val remoteNote = remoteMap[id]

            when {
                localNote != null && remoteNote == null -> {
                    s3.saveNote(localNote)
                }
                localNote == null && remoteNote != null -> {
                    local.saveNote(remoteNote)
                }
                localNote != null && remoteNote != null -> {
                    if (localNote.updatedAt > remoteNote.updatedAt) {
                        s3.saveNote(localNote)
                    } else if (remoteNote.updatedAt > localNote.updatedAt) {
                        local.saveNote(remoteNote)
                    }
                }
            }
        }
    }
}
