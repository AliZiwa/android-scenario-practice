package dev.aliziwa.partsbin.ui.screens

import app.cash.turbine.test
import dev.aliziwa.partsbin.MainDispatcherRule
import dev.aliziwa.partsbin.domain.PartRow
import dev.aliziwa.partsbin.ui.FakePartsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PartsListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakePartsRepository
    private lateinit var viewModel: PartsListViewModel

    @Before
    fun setUp() {
        repository = FakePartsRepository()
        viewModel = PartsListViewModel(repository, UnconfinedTestDispatcher())
    }

    @Test
    fun `initial load gets parts from repository`() = runTest {
        val testPart = PartRow("1", "SKU1", "Part 1", 10, 5, "Oct 27", "A1", false, 1)
        repository.setParts(listOf(testPart))

        viewModel.uiState.test {
            val state = awaitItem() as PartsListState.Loaded
            assertEquals(1, state.rows.size)
            assertEquals("SKU1", state.rows[0].sku)
        }
    }

    @Test
    fun `load network failure shows cached parts with offline message`() = runTest {
        repository.shouldFailRefresh = true
        val testPart = PartRow("1", "SKU1", "Part 1", 10, 5, "Oct 27", "A1", false, 1)
        repository.setParts(listOf(testPart))
        
        viewModel.load()

        viewModel.uiState.test {
            val state = awaitItem() as PartsListState.Loaded
            assertEquals(1, state.rows.size)
            assertEquals("Offline, showing cached parts", state.message)
        }
    }

    @Test
    fun `query filtering updates parts list`() = runTest {
        val p1 = PartRow("1", "SKU1", "Hammer", 10, 5, "Oct 27", "A1", false, 1)
        val p2 = PartRow("2", "SKU2", "Nail", 50, 10, "Oct 27", "A2", false, 1)
        repository.setParts(listOf(p1, p2))

        viewModel.uiState.test {
            val initialState = awaitItem() as PartsListState.Loaded
            assertEquals(2, initialState.rows.size)

            viewModel.onQueryChange("hammer")
            val filteredState = awaitItem() as PartsListState.Loaded
            assertEquals(1, filteredState.rows.size)
            assertEquals("Hammer", filteredState.rows[0].name)
        }
    }
}
