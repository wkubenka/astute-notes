package com.astute.notes.ui.noteeditor

import com.astute.notes.data.FakeNoteRepository
import com.astute.notes.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoteEditorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakeNoteRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeNoteRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): NoteEditorViewModel {
        return NoteEditorViewModel(repository = fakeRepo)
    }

    @Test
    fun `initial state is empty`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertEquals("", state.title)
        assertEquals("", state.body)
        assertFalse(state.isLoading)
        assertFalse(state.isSaving)
        assertFalse(state.isSaved)
        assertNull(state.error)
    }

    @Test
    fun `isNewNote returns true when no note loaded`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        assertTrue(viewModel.isNewNote)
    }

    @Test
    fun `loadNote populates state from repository`() = runTest(testDispatcher) {
        val note = Note("1", "Title", "Body", 100L, 200L)
        fakeRepo.seed(note)

        val viewModel = createViewModel()
        viewModel.loadNote("1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Title", state.title)
        assertEquals("Body", state.body)
        assertFalse(state.isLoading)
        assertFalse(viewModel.isNewNote)
    }

    @Test
    fun `loadNote with null id does nothing`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.loadNote(null)
        advanceUntilIdle()

        assertTrue(viewModel.isNewNote)
        assertEquals("", viewModel.uiState.value.title)
    }

    @Test
    fun `loadNote sets error when note not found`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.loadNote("nonexistent")
        advanceUntilIdle()

        assertEquals("Note not found", viewModel.uiState.value.error)
    }

    @Test
    fun `loadNote sets error on repository failure`() = runTest(testDispatcher) {
        fakeRepo.shouldThrow = true

        val viewModel = createViewModel()
        viewModel.loadNote("1")
        advanceUntilIdle()

        assertEquals("Fake error", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `onTitleChanged updates title in state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onTitleChanged("New Title")
        assertEquals("New Title", viewModel.uiState.value.title)
    }

    @Test
    fun `onBodyChanged updates body in state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onBodyChanged("New Body")
        assertEquals("New Body", viewModel.uiState.value.body)
    }

    @Test
    fun `saveNote creates new note when no existing note`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onTitleChanged("My Title")
        viewModel.onBodyChanged("My Body")

        viewModel.saveNote()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isSaved)
        assertFalse(state.isSaving)
        assertNull(state.error)

        val notes = fakeRepo.allNotes()
        assertEquals(1, notes.size)
        assertEquals("My Title", notes[0].title)
        assertEquals("My Body", notes[0].body)
    }

    @Test
    fun `saveNote updates existing note`() = runTest(testDispatcher) {
        val note = Note("1", "Old Title", "Old Body", 100L, 200L)
        fakeRepo.seed(note)

        val viewModel = createViewModel()
        viewModel.loadNote("1")
        advanceUntilIdle()

        viewModel.onTitleChanged("Updated Title")
        viewModel.onBodyChanged("Updated Body")
        viewModel.saveNote()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)

        val saved = fakeRepo.getNoteById("1")!!
        assertEquals("Updated Title", saved.title)
        assertEquals("Updated Body", saved.body)
    }

    @Test
    fun `saveNote sets error on failure`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onTitleChanged("Title")

        fakeRepo.shouldThrow = true
        viewModel.saveNote()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertFalse(state.isSaved)
        assertEquals("Fake error", state.error)
    }

}
