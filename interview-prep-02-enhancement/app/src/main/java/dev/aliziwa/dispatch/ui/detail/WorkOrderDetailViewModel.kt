package dev.aliziwa.dispatch.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aliziwa.dispatch.data.WorkOrdersRepository
import dev.aliziwa.dispatch.domain.Status
import dev.aliziwa.dispatch.domain.WorkOrder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface WorkOrderDetailUiState {
    data object Loading : WorkOrderDetailUiState
    data class Loaded(val workOrder: WorkOrder) : WorkOrderDetailUiState
    data object Gone : WorkOrderDetailUiState
}

class WorkOrderDetailViewModel(
    private val workOrderId: String,
    private val repository: WorkOrdersRepository,
) : ViewModel() {

    val uiState: StateFlow<WorkOrderDetailUiState> = repository
        .observeWorkOrder(workOrderId)
        .map { order ->
            if (order == null) WorkOrderDetailUiState.Gone else WorkOrderDetailUiState.Loaded(order)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            WorkOrderDetailUiState.Loading
        )

    fun updateStatus() {
        if (uiState.value !is WorkOrderDetailUiState.Loaded) return
        val currentWorkOrder = (uiState.value as WorkOrderDetailUiState.Loaded).workOrder

        viewModelScope.launch {
            repository.updateWorkOrderStatus(
                currentWorkOrder.copy(
                    isPending = true,
                    status = when (currentWorkOrder.status) {
                        Status.OPEN -> Status.IN_PROGRESS
                        Status.IN_PROGRESS -> Status.DONE
                        else -> return@launch
                    }
                )
            )
        }
    }
}
