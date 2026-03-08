package com.astute.notes.data

import com.astute.notes.model.Note
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncingNoteRepositoryTest {

    private lateinit var local: FakeNoteRepository
    private lateinit var remote: FakeNoteRepository
    private lateinit var repo: SyncingNoteRepository

    @Before
    fun setUp() {
        local = FakeNoteRepository()
        remote = FakeNoteRepository()
        repo = SyncingNoteRepository(local)
        repo.remote = remote
    }

    @Test
    fun `reads come from local`() = runTest {
        local.seed(Note("1", "Local", "Body", 100L, 200L))
        remote.seed(Note("2", "Remote", "Body", 100L, 200L))

        val notes = repo.listNotes()
        assertEquals(1, notes.size)
        assertEquals("Local", notes[0].title)
    }

    @Test
    fun `createNote saves to both local and remote`() = runTest {
        repo.createNote("Title", "Body")

        assertEquals(1, local.allNotes().size)
        assertEquals(1, remote.allNotes().size)
        assertEquals(local.allNotes()[0].id, remote.allNotes()[0].id)
    }

    @Test
    fun `updateNote saves to both local and remote`() = runTest {
        local.seed(Note("1", "Old", "Body", 100L, 200L))

        repo.updateNote(Note("1", "New", "Body", 100L, 200L))

        assertEquals("New", local.getNoteById("1")!!.title)
        assertEquals("New", remote.getNoteById("1")!!.title)
    }

    @Test
    fun `deleteNote removes from both local and remote`() = runTest {
        local.seed(Note("1", "Title", "Body", 100L, 200L))
        remote.seed(Note("1", "Title", "Body", 100L, 200L))

        repo.deleteNote("1")

        assertNull(local.getNoteById("1"))
        assertNull(remote.getNoteById("1"))
    }

    @Test
    fun `remote failure does not fail local create`() = runTest {
        remote.shouldThrow = true

        val note = repo.createNote("Title", "Body")

        assertEquals(1, local.allNotes().size)
        assertEquals("Title", local.getNoteById(note.id)!!.title)
    }

    @Test
    fun `remote failure does not fail local delete`() = runTest {
        local.seed(Note("1", "Title", "Body", 100L, 200L))
        remote.shouldThrow = true

        repo.deleteNote("1")

        assertNull(local.getNoteById("1"))
    }

    // --- sync tests ---

    @Test
    fun `sync pulls remote-only notes to local`() = runTest {
        remote.seed(Note("1", "Remote Note", "Body", 100L, 200L))

        repo.sync()

        assertEquals("Remote Note", local.getNoteById("1")!!.title)
    }

    @Test
    fun `sync pushes local-only notes to remote`() = runTest {
        local.seed(Note("1", "Local Note", "Body", 100L, 200L))

        repo.sync()

        assertEquals("Local Note", remote.getNoteById("1")!!.title)
    }

    @Test
    fun `sync keeps newer local note`() = runTest {
        local.seed(Note("1", "Newer Local", "Body", 100L, 300L))
        remote.seed(Note("1", "Older Remote", "Body", 100L, 200L))

        repo.sync()

        assertEquals("Newer Local", local.getNoteById("1")!!.title)
        assertEquals("Newer Local", remote.getNoteById("1")!!.title)
    }

    @Test
    fun `sync keeps newer remote note`() = runTest {
        local.seed(Note("1", "Older Local", "Body", 100L, 200L))
        remote.seed(Note("1", "Newer Remote", "Body", 100L, 300L))

        repo.sync()

        assertEquals("Newer Remote", local.getNoteById("1")!!.title)
        assertEquals("Newer Remote", remote.getNoteById("1")!!.title)
    }

    @Test
    fun `sync with equal timestamps is a no-op`() = runTest {
        local.seed(Note("1", "Same", "Body", 100L, 200L))
        remote.seed(Note("1", "Same", "Body", 100L, 200L))

        repo.sync()

        assertEquals("Same", local.getNoteById("1")!!.title)
        assertEquals("Same", remote.getNoteById("1")!!.title)
    }

    @Test
    fun `sync with no remote configured is a no-op`() = runTest {
        repo.remote = null
        local.seed(Note("1", "Title", "Body", 100L, 200L))

        repo.sync()

        assertEquals(1, local.allNotes().size)
        assertTrue(remote.allNotes().isEmpty())
    }
}
