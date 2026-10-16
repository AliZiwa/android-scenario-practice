package dev.aliziwa.dispatch.ui.detail

import app.cash.turbine.test
import dev.aliziwa.dispatch.FakeWorkOrdersRepository
import dev.aliziwa.dispatch.MainDispatcherRule
import dev.aliziwa.dispatch.data.UpdateWorkOrderResult
import dev.aliziwa.dispatch.domain.Priority
import dev.aliziwa.dispatch.domain.Status
import dev.aliziwa.dispatch.domain.WorkOrder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WorkOrderDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeWorkOrdersRepository()

    @Test
    fun `loads work order detail successfully`() = runTest {
        repository.workOrders.value = listOf(order("wo-101", status = Status.OPEN))

        val viewModel = WorkOrderDetailViewModel("wo-101", repository)

        viewModel.uiState.test {
            val loadedState = awaitItem() as WorkOrderDetailUiState.Loaded
            assertEquals("wo-101", loadedState.workOrder.id)
            assertEquals(Status.OPEN, loadedState.workOrder.status)
            assertNull(loadedState.message)
        }
    }

    @Test
    fun `updateStatus when OPEN updates status to IN_PROGRESS optimistically with pending sync`() = runTest {
        repository.workOrders.value = listOf(order("wo-101", status = Status.OPEN))

        val viewModel = WorkOrderDetailViewModel("wo-101", repository)

        viewModel.uiState.test {
            val initial = awaitItem() as WorkOrderDetailUiState.Loaded
            assertEquals(Status.OPEN, initial.workOrder.status)

            viewModel.updateStatus()

            val updated = awaitItem() as WorkOrderDetailUiState.Loaded
            assertEquals(Status.IN_PROGRESS, updated.workOrder.status)
            assertTrue(updated.workOrder.isPending)
            assertEquals(1, repository.updatedWorkOrders.size)
            assertEquals(Status.IN_PROGRESS, repository.updatedWorkOrders.first().status)
            assertTrue(repository.updatedWorkOrders.first().isPending)
        }
    }

    @Test
    fun `updateStatus when IN_PROGRESS updates status to DONE optimistically with pending sync`() = runTest {
        repository.workOrders.value = listOf(order("wo-101", status = Status.IN_PROGRESS))

        val viewModel = WorkOrderDetailViewModel("wo-101", repository)

        viewModel.uiState.test {
            val initial = awaitItem() as WorkOrderDetailUiState.Loaded
            assertEquals(Status.IN_PROGRESS, initial.workOrder.status)

            viewModel.updateStatus()

            val updated = awaitItem() as WorkOrderDetailUiState.Loaded
            assertEquals(Status.DONE, updated.workOrder.status)
            assertTrue(updated.workOrder.isPending)
            assertEquals(1, repository.updatedWorkOrders.size)
            assertEquals(Status.DONE, repository.updatedWorkOrders.first().status)
            assertTrue(repository.updatedWorkOrders.first().isPending)
        }
    }

    @Test
    fun `updateStatus when Conflict sets error message`() = runTest {
        repository.workOrders.value = listOf(order("wo-101", status = Status.OPEN))
        repository.updateResult = UpdateWorkOrderResult.Conflict

        val viewModel = WorkOrderDetailViewModel("wo-101", repository)

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.updateStatus()

            val stateWithMessage = awaitItem() as WorkOrderDetailUiState.Loaded
            assertEquals("Updated by dispatcher, review and try again", stateWithMessage.message)
        }
    }

    @Test
    fun `updateStatus when Rejected sets rejection reason message`() = runTest {
        repository.workOrders.value = listOf(order("wo-101", status = Status.OPEN))
        repository.updateResult = UpdateWorkOrderResult.Rejected("invalid_transition")

        val viewModel = WorkOrderDetailViewModel("wo-101", repository)

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.updateStatus()

            val stateWithMessage = awaitItem() as WorkOrderDetailUiState.Loaded
            assertEquals("invalid_transition", stateWithMessage.message)
        }
    }

    @Test
    fun `updateStatus when DONE does nothing`() = runTest {
        repository.workOrders.value = listOf(order("wo-101", status = Status.DONE))

        val viewModel = WorkOrderDetailViewModel("wo-101", repository)

        viewModel.uiState.test {
            val initial = awaitItem() as WorkOrderDetailUiState.Loaded
            assertEquals(Status.DONE, initial.workOrder.status)

            viewModel.updateStatus()

            assertTrue(repository.updatedWorkOrders.isEmpty())
        }
    }

    private fun order(
        id: String,
        status: Status = Status.OPEN,
        isPending: Boolean = false
    ) = WorkOrder(
        id = id,
        title = "Title $id",
        site = "Bay 1",
        priority = Priority.HIGH,
        status = status,
        version = 1,
        updatedAt = 0,
        isPending = isPending,
    )
}
