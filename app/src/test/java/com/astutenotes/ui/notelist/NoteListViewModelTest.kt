package com.astutenotes.ui.notelist

import com.astutenotes.data.FakeNoteRepository
import com.astutenotes.model.Note
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
class NoteListViewModelTest {

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

    private fun createViewModel(): NoteListViewModel {
        return NoteListViewModel(repository = fakeRepo)
    }

    @Test
    fun `init loads notes from repository`() = runTest(testDispatcher) {
        val note = Note("1", "Title", "Body", 100L, 200L)
        fakeRepo.seed(note)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.notes.size)
        assertEquals("Title", state.notes[0].title)
        assertNull(state.error)
    }

    @Test
    fun `init with empty repository shows empty list`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.notes.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadNotes sets error on failure`() = runTest(testDispatcher) {
        fakeRepo.shouldThrow = true

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Fake error", state.error)
    }

    @Test
    fun `deleteNote removes note and reloads`() = runTest(testDispatcher) {
        val note = Note("1", "Title", "Body", 100L, 200L)
        fakeRepo.seed(note)

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.notes.size)

        viewModel.deleteNote("1")
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.notes.size)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `deleteNote sets error on failure`() = runTest(testDispatcher) {
        val note = Note("1", "Title", "Body", 100L, 200L)
        fakeRepo.seed(note)

        val viewModel = createViewModel()
        advanceUntilIdle()

        fakeRepo.shouldThrow = true
        viewModel.deleteNote("1")
        advanceUntilIdle()

        assertEquals("Fake error", viewModel.uiState.value.error)
    }

    @Test
    fun `loadNotes clears previous error`() = runTest(testDispatcher) {
        fakeRepo.shouldThrow = true
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals("Fake error", viewModel.uiState.value.error)

        fakeRepo.shouldThrow = false
        viewModel.loadNotes()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `multiple notes are returned sorted by updatedAt`() = runTest(testDispatcher) {
        fakeRepo.seed(
            Note("1", "Old", "Body", 100L, 100L),
            Note("2", "New", "Body", 200L, 300L),
            Note("3", "Mid", "Body", 150L, 200L)
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val titles = viewModel.uiState.value.notes.map { it.title }
        assertEquals(listOf("New", "Mid", "Old"), titles)
    }
}
