package com.astute.notes.data

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class LocalNoteRepositoryTest {

    private lateinit var tempDir: File
    private lateinit var repo: LocalNoteRepository

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "notes-test-${System.nanoTime()}")
        tempDir.mkdirs()
        repo = LocalNoteRepository(tempDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `listNotes returns empty list when no notes exist`() = runTest {
        val notes = repo.listNotes()
        assertTrue(notes.isEmpty())
    }

    @Test
    fun `createNote stores a note and returns it`() = runTest {
        val note = repo.createNote("Title", "Body")

        assertEquals("Title", note.title)
        assertEquals("Body", note.body)
        assertTrue(note.id.isNotBlank())
        assertTrue(note.createdAt > 0)
        assertEquals(note.createdAt, note.updatedAt)

        // Verify file was written
        val file = File(tempDir, "${note.id}.json")
        assertTrue(file.exists())
    }

    @Test
    fun `getNoteById returns stored note`() = runTest {
        val created = repo.createNote("Title", "Body")
        val fetched = repo.getNoteById(created.id)

        assertNotNull(fetched)
        assertEquals(created.id, fetched!!.id)
        assertEquals("Title", fetched.title)
        assertEquals("Body", fetched.body)
    }

    @Test
    fun `getNoteById returns null for missing note`() = runTest {
        val result = repo.getNoteById("nonexistent")
        assertNull(result)
    }

    @Test
    fun `listNotes returns all stored notes sorted by updatedAt descending`() = runTest {
        val note1 = repo.createNote("First", "Body 1")
        Thread.sleep(10) // ensure different timestamps
        val note2 = repo.createNote("Second", "Body 2")

        val notes = repo.listNotes()
        assertEquals(2, notes.size)
        assertEquals(note2.id, notes[0].id) // most recent first
        assertEquals(note1.id, notes[1].id)
    }

    @Test
    fun `updateNote modifies the note and updates timestamp`() = runTest {
        val created = repo.createNote("Title", "Body")
        Thread.sleep(10)
        val updated = repo.updateNote(created.copy(title = "New Title", body = "New Body"))

        assertEquals(created.id, updated.id)
        assertEquals("New Title", updated.title)
        assertEquals("New Body", updated.body)
        assertTrue(updated.updatedAt > created.updatedAt)

        // Verify persisted
        val fetched = repo.getNoteById(created.id)
        assertEquals("New Title", fetched!!.title)
    }

    @Test
    fun `deleteNote removes the note`() = runTest {
        val note = repo.createNote("Title", "Body")
        assertEquals(1, repo.listNotes().size)

        repo.deleteNote(note.id)

        assertEquals(0, repo.listNotes().size)
        assertNull(repo.getNoteById(note.id))
        assertTrue(!File(tempDir, "${note.id}.json").exists())
    }

    @Test
    fun `listNotes skips malformed json files`() = runTest {
        // Create a valid note
        repo.createNote("Valid", "Note")

        // Write a malformed JSON file
        File(tempDir, "bad-note.json").writeText("not valid json{{{")

        val notes = repo.listNotes()
        assertEquals(1, notes.size)
        assertEquals("Valid", notes[0].title)
    }

    @Test
    fun `listNotes ignores non-json files`() = runTest {
        repo.createNote("Title", "Body")
        File(tempDir, "readme.txt").writeText("not a note")

        val notes = repo.listNotes()
        assertEquals(1, notes.size)
    }

    @Test
    fun `createNote generates unique ids`() = runTest {
        val note1 = repo.createNote("A", "Body")
        val note2 = repo.createNote("B", "Body")
        assertTrue(note1.id != note2.id)
    }

    @Test
    fun `creates notes directory if it does not exist`() {
        tempDir.deleteRecursively()
        val repo2 = LocalNoteRepository(tempDir)
        assertTrue(tempDir.exists())
    }
}
