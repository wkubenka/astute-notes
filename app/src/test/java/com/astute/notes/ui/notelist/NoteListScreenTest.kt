package com.astute.notes.ui.notelist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.astute.notes.data.FakeNoteRepository
import com.astute.notes.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class NoteListScreenTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val composeTestRule = createComposeRule()

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

    private fun setUpScreen(vararg notes: Note) {
        fakeRepo.seed(*notes)
        val viewModel = NoteListViewModel(repository = fakeRepo)

        composeTestRule.setContent {
            NoteListScreen(
                onCreateNote = {},
                onEditNote = {},
                onOpenSettings = {},
                viewModel = viewModel
            )
        }

        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `clicking delete shows confirmation dialog`() {
        setUpScreen(Note("1", "My Note", "Body", 100L, 200L))

        composeTestRule.onNodeWithContentDescription("Delete note").performClick()

        composeTestRule.onNodeWithText("Delete note?").assertIsDisplayed()
        composeTestRule.onNodeWithText("\"My Note\" will be permanently deleted.").assertIsDisplayed()
    }

    @Test
    fun `cancel dismisses dialog without deleting`() {
        setUpScreen(Note("1", "My Note", "Body", 100L, 200L))

        composeTestRule.onNodeWithContentDescription("Delete note").performClick()
        composeTestRule.onNodeWithText("Cancel").performClick()

        composeTestRule.onNodeWithText("Delete note?").assertDoesNotExist()
        composeTestRule.onNodeWithText("My Note").assertIsDisplayed()
    }

    @Test
    fun `confirming delete removes the note`() {
        setUpScreen(Note("1", "My Note", "Body", 100L, 200L))

        composeTestRule.onNodeWithContentDescription("Delete note").performClick()
        composeTestRule.onNodeWithText("Delete").performClick()

        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("My Note").assertDoesNotExist()
        composeTestRule.onNodeWithText("No notes yet. Tap + to create one.").assertIsDisplayed()
    }
}
