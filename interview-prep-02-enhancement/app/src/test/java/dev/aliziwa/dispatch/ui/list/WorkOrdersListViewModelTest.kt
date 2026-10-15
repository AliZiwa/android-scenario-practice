package dev.aliziwa.dispatch.ui.list

import app.cash.turbine.test
import dev.aliziwa.dispatch.FakeWorkOrdersRepository
import dev.aliziwa.dispatch.MainDispatcherRule
import dev.aliziwa.dispatch.domain.Priority
import dev.aliziwa.dispatch.domain.Status
import dev.aliziwa.dispatch.domain.WorkOrder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class WorkOrdersListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeWorkOrdersRepository()

    @Test
    fun `refreshes once on creation and reports the count`() = runTest {
        repository.workOrders.value = listOf(order("wo-1"))

        val viewModel = WorkOrdersListViewModel(repository)

        assertEquals(1, repository.refreshCalls)
        assertEquals(1, viewModel.uiState.value.lastRefreshCount)
        assertEquals(listOf("wo-1"), viewModel.uiState.value.workOrders.map { it.id })
    }

    @Test
    fun `offline refresh keeps the cached list and shows an error`() = runTest {
        repository.workOrders.value = listOf(order("wo-1"))
        repository.refreshError = IOException("no route")

        val viewModel = WorkOrdersListViewModel(repository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isRefreshing)
            assertEquals("Offline: showing cached work orders", state.error)
            assertEquals(1, state.workOrders.size)
        }
    }

    @Test
    fun `a refresh in flight blocks a second one`() = runTest {
        val gate = CompletableDeferred<Unit>()
        repository.refreshGate = gate

        val viewModel = WorkOrdersListViewModel(repository)
        assertTrue(viewModel.uiState.value.isRefreshing)

        viewModel.refresh()
        assertEquals(1, repository.refreshCalls)

        gate.complete(Unit)
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertNull(viewModel.uiState.value.error)
    }

    private fun order(id: String) = WorkOrder(
        id = id,
        title = "Title $id",
        site = "Bay 1",
        priority = Priority.HIGH,
        status = Status.OPEN,
        version = 1,
        updatedAt = 0,
    )
}
