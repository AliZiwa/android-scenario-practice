package dev.aliziwa.partsbin.ui.screens

import app.cash.turbine.test
import dev.aliziwa.partsbin.MainDispatcherRule
import dev.aliziwa.partsbin.domain.Part
import dev.aliziwa.partsbin.domain.RefreshResult
import dev.aliziwa.partsbin.ui.FakePartsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PartsListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakePartsRepository()

    @Test
    fun `initial load succeeds and filters by query on sku`() = runTest {
        val part1 = Part("p1", "SKU1", "Name", 10, 5, "Bin", "2026-10-27T14:03:00Z")
        val part2 = Part("p2", "XYZ9", "Other", 10, 5, "Bin", "2026-10-27T14:03:00Z")
        repository.rows.value = mapOf("p1" to part1, "p2" to part2)

        val vm = PartsListViewModel(repository)

        vm.uiState.test {
            val loadingState = awaitItem()
            val initialState = if (loadingState is PartsListState.Loading) awaitItem() else loadingState
            assertTrue(initialState is PartsListState.Loaded)
            assertEquals(2, (initialState as PartsListState.Loaded).rows.size)

            vm.onQueryChange("sku1")
            val filteredState = awaitItem()
            assertTrue(filteredState is PartsListState.Loaded)
            assertEquals(1, (filteredState as PartsListState.Loaded).rows.size)
            
            vm.onQueryChange("none")
            val noMatchState = awaitItem()
            assertTrue(noMatchState is PartsListState.Loaded)
            assertEquals(0, (noMatchState as PartsListState.Loaded).rows.size)
        }
    }
    
    @Test
    fun `initial load succeeds and filters by query on name`() = runTest {
        val part1 = Part("p1", "SKU1", "Hammer", 10, 5, "Bin", "2026-10-27T14:03:00Z")
        val part2 = Part("p2", "SKU2", "Wrench", 10, 5, "Bin", "2026-10-27T14:03:00Z")
        repository.rows.value = mapOf("p1" to part1, "p2" to part2)

        val vm = PartsListViewModel(repository)

        vm.uiState.test {
            val loadingState = awaitItem()
            val initialState = if (loadingState is PartsListState.Loading) awaitItem() else loadingState
            assertTrue(initialState is PartsListState.Loaded)
            assertEquals(2, (initialState as PartsListState.Loaded).rows.size)

            vm.onQueryChange("hammer")
            val filteredState = awaitItem()
            assertTrue(filteredState is PartsListState.Loaded)
            assertEquals(1, (filteredState as PartsListState.Loaded).rows.size)
        }
    }

    @Test
    fun `offline fallback message`() = runTest {
        repository.refreshResult = RefreshResult.Offline
        val vm = PartsListViewModel(repository)

        vm.uiState.test {
            val loadingState = awaitItem()
            val initialState = if (loadingState is PartsListState.Loading) awaitItem() else loadingState
            assertTrue(initialState is PartsListState.Loaded)
            
            // Note: Error message can arrive after Loaded event due to combine
            val finalState = if ((initialState as PartsListState.Loaded).message == null) awaitItem() else initialState
            assertEquals("Offline, showing cached parts", (finalState as PartsListState.Loaded).message)
        }
    }
    
    @Test
    fun `could not refresh fallback message`() = runTest {
        repository.refreshResult = RefreshResult.Error("Server error")
        val vm = PartsListViewModel(repository)

        vm.uiState.test {
            val loadingState = awaitItem()
            val initialState = if (loadingState is PartsListState.Loading) awaitItem() else loadingState
            assertTrue(initialState is PartsListState.Loaded)
            
            val finalState = if ((initialState as PartsListState.Loaded).message == null) awaitItem() else initialState
            assertEquals("Could not refresh", (finalState as PartsListState.Loaded).message)
        }
    }
}
