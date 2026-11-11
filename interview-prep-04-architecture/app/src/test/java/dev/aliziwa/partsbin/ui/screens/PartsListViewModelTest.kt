package dev.aliziwa.partsbin.ui.screens

import app.cash.turbine.test
import dev.aliziwa.partsbin.MainDispatcherRule
import dev.aliziwa.partsbin.domain.Part
import dev.aliziwa.partsbin.ui.FakePartsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class PartsListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakePartsRepository()

    @Test
    fun `initial load succeeds and filters by query`() = runTest {
        val part = Part("p1", "SKU1", "Name", 10, 5, "Bin", "2026-10-27T14:03:00Z")
        repository.rows.value = mapOf("p1" to part)

        val vm = PartsListViewModel(repository, mainDispatcherRule.dispatcher)

        vm.uiState.test {
            val initialState = awaitItem()
            assertTrue(initialState is PartsListState.Loaded)
            assertEquals(1, (initialState as PartsListState.Loaded).rows.size)

            vm.onQueryChange("XYZ")
            val filteredState = awaitItem()
            assertTrue(filteredState is PartsListState.Loaded)
            assertEquals(0, (filteredState as PartsListState.Loaded).rows.size)
        }
    }

    @Test
    fun `offline fallback message`() = runTest {
        repository.throwOnRefresh = IOException("network")
        val vm = PartsListViewModel(repository, mainDispatcherRule.dispatcher)

        vm.uiState.test {
            val initialState = awaitItem()
            assertTrue(initialState is PartsListState.Loaded)
            assertEquals("Offline, showing cached parts", (initialState as PartsListState.Loaded).message)
        }
    }
}
