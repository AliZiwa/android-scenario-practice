package dev.aliziwa.fieldnotes.ui.list

import app.cash.turbine.test
import dev.aliziwa.fieldnotes.FakeNotesRepository
import dev.aliziwa.fieldnotes.MainDispatcherRule
import dev.aliziwa.fieldnotes.ui.detail.NoteDetailViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotesListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeNotesRepository()

    @Test
    fun `starts a sync on creation`() = runTest {
        val viewModel = NotesListViewModel(repository)

        assertEquals(1, repository.syncCalls)
        assertEquals(1, viewModel.uiState.value.syncCount)
        assertTrue(viewModel.uiState.value.notes.isEmpty())
    }


    @Test
    fun `on delete removes item from list`() = runTest {
        val addedNoteId = repository.create("title", "body")

        val viewModel = NoteDetailViewModel(addedNoteId, repository)
        viewModel.delete()

        assertTrue(viewModel.note.value == null)

    }

    @Test
    fun `on sync uiState updates after completion of sync`() = runTest {
        val viewModel = NotesListViewModel(repository)

        viewModel.sync()
        viewModel.sync()
        viewModel.sync()
        viewModel.sync()

        viewModel.uiState.test {
            val item = awaitItem()
            assertEquals(5, item.syncCount)
            assertEquals(false, item.isSyncing)
        }

    }

    // Add your regression tests below. Each defect you fix should have a test that fails on main
    // and passes on your branch. Turbine is on the test classpath for Flow assertions.
}
