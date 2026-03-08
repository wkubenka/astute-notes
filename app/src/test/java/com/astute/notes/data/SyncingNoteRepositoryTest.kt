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

    @Test
    fun `saveNote writes to both local and remote`() = runTest {
        val note = Note("1", "Saved", "Body", 100L, 200L)

        repo.saveNote(note)

        assertEquals("Saved", local.getNoteById("1")!!.title)
        assertEquals("Saved", remote.getNoteById("1")!!.title)
    }

    @Test
    fun `remote failure does not fail local saveNote`() = runTest {
        remote.shouldThrow = true
        val note = Note("1", "Saved", "Body", 100L, 200L)

        repo.saveNote(note)

        assertEquals("Saved", local.getNoteById("1")!!.title)
    }

    @Test
    fun `remote failure does not fail local update`() = runTest {
        local.seed(Note("1", "Old", "Body", 100L, 200L))
        remote.shouldThrow = true

        repo.updateNote(Note("1", "New", "Body", 100L, 200L))

        assertEquals("New", local.getNoteById("1")!!.title)
    }

    @Test
    fun `sync handles mixed local-only remote-only and shared notes`() = runTest {
        local.seed(
            Note("local-only", "Local Only", "Body", 100L, 200L),
            Note("shared", "Older Local", "Body", 100L, 200L)
        )
        remote.seed(
            Note("remote-only", "Remote Only", "Body", 100L, 200L),
            Note("shared", "Newer Remote", "Body", 100L, 300L)
        )

        repo.sync()

        // local-only pushed to remote
        assertEquals("Local Only", remote.getNoteById("local-only")!!.title)
        // remote-only pulled to local
        assertEquals("Remote Only", local.getNoteById("remote-only")!!.title)
        // shared resolved to newer (remote)
        assertEquals("Newer Remote", local.getNoteById("shared")!!.title)
        assertEquals("Newer Remote", remote.getNoteById("shared")!!.title)
    }

    @Test
    fun `sync continues past individual note failures`() = runTest {
        local.seed(Note("ok", "Will Sync", "Body", 100L, 200L))
        // "fail" only exists in local, so sync will try to push it to remote
        local.seed(Note("fail", "Will Fail", "Body", 100L, 200L))
        // remote rejects saves for "fail"
        remote.throwOnSaveIds = setOf("fail")

        repo.sync()

        // "ok" should still sync despite "fail" erroring
        assertEquals("Will Sync", remote.getNoteById("ok")!!.title)
        // "fail" was not pushed (remote rejected it) but local is untouched
        assertEquals("Will Fail", local.getNoteById("fail")!!.title)
        assertNull(remote.getNoteById("fail"))
    }
}
