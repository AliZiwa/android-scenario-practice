package dev.aliziwa.dispatch.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aliziwa.dispatch.data.UpdateWorkOrderResult
import dev.aliziwa.dispatch.data.WorkOrdersRepository
import dev.aliziwa.dispatch.domain.Status
import dev.aliziwa.dispatch.domain.WorkOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface WorkOrderDetailUiState {
    data object Loading : WorkOrderDetailUiState
    data class Loaded(
        val workOrder: WorkOrder,
        val message: String? = null,
    ) : WorkOrderDetailUiState
    data object Gone : WorkOrderDetailUiState
}

class WorkOrderDetailViewModel(
    private val workOrderId: String,
    private val repository: WorkOrdersRepository,
) : ViewModel() {

    private val userMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<WorkOrderDetailUiState> = repository
        .observeWorkOrder(workOrderId)
        .combine(userMessage) { order, msg ->
            if (order == null) {
                WorkOrderDetailUiState.Gone
            } else {
                WorkOrderDetailUiState.Loaded(workOrder = order, message = msg)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            WorkOrderDetailUiState.Loading
        )

    fun updateStatus() {
        val currentLoadedState = uiState.value as? WorkOrderDetailUiState.Loaded ?: return
        val currentWorkOrder = currentLoadedState.workOrder
        val nextStatus = currentWorkOrder.status.next() ?: return

        userMessage.value = null

        viewModelScope.launch {
            val result = repository.updateWorkOrderStatus(
                currentWorkOrder.copy(
                    isPending = true,
                    status = nextStatus,
                )
            )
            when (result) {
                is UpdateWorkOrderResult.Conflict -> {
                    userMessage.value = "Updated by dispatcher, review and try again"
                }
                is UpdateWorkOrderResult.Rejected -> {
                    userMessage.value = result.reason
                }
                UpdateWorkOrderResult.Applied,
                UpdateWorkOrderResult.Pending -> {
                    userMessage.value = null
                }
            }
        }
    }
}
