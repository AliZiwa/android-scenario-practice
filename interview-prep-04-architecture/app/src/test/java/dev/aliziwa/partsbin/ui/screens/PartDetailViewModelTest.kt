package dev.aliziwa.partsbin.ui.screens

import app.cash.turbine.test
import dev.aliziwa.partsbin.MainDispatcherRule
import dev.aliziwa.partsbin.domain.PartRow
import dev.aliziwa.partsbin.ui.FakePartsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PartDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakePartsRepository
    private lateinit var viewModel: PartDetailViewModel

    @Before
    fun setUp() {
        repository = FakePartsRepository()
        viewModel = PartDetailViewModel(repository, UnconfinedTestDispatcher())
    }

    @Test
    fun `load fetches part details`() = runTest {
        val testPart = PartRow("1", "SKU1", "Part 1", 10, 5, "Oct 27", "A1", false, 1)
        repository.setParts(listOf(testPart))

        viewModel.load("1")

        viewModel.uiState.test {
            val state = awaitItem() as PartsDetailState.Loaded
            assertEquals("SKU1", state.rowItem.sku)
        }
    }

    @Test
    fun `adjust success updates state`() = runTest {
        val testPart = PartRow("1", "SKU1", "Part 1", 10, 5, "Oct 27", "A1", false, 1)
        repository.setParts(listOf(testPart))

        viewModel.load("1")
        viewModel.adjust("1", -2, "Used")

        viewModel.uiState.test {
            val state = awaitItem() as PartsDetailState.Loaded
            assertEquals(8, state.rowItem.quantity)
        }
    }

    @Test
    fun `adjust invalid delta shows error state`() = runTest {
        val testPart = PartRow("1", "SKU1", "Part 1", 10, 5, "Oct 27", "A1", false, 1)
        repository.setParts(listOf(testPart))
        repository.adjustException = IllegalArgumentException("Not enough stock")

        viewModel.load("1")
        viewModel.adjust("1", -15, "Used")

        viewModel.uiState.test {
            val state = awaitItem() as PartsDetailState.Error
            assertEquals("Not enough stock", state.message)
        }
    }

    @Test
    fun `adjust network error shows offline message`() = runTest {
        val testPart = PartRow("1", "SKU1", "Part 1", 10, 5, "Oct 27", "A1", false, 1)
        repository.setParts(listOf(testPart))
        repository.adjustException = IOException("Network error")

        viewModel.load("1")
        viewModel.adjust("1", -2, "Used")

        viewModel.uiState.test {
            val state = awaitItem() as PartsDetailState.Error
            assertEquals("Offline, adjustment not saved", state.message)
        }
    }
}
