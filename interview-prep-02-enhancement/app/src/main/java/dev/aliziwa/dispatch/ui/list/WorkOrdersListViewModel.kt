package dev.aliziwa.dispatch.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aliziwa.dispatch.data.WorkOrdersRepository
import dev.aliziwa.dispatch.domain.WorkOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class WorkOrdersListUiState(
    val workOrders: List<WorkOrder> = emptyList(),
    val isRefreshing: Boolean = false,
    val lastRefreshCount: Int? = null,
    val error: String? = null,
)

class WorkOrdersListViewModel(
    private val repository: WorkOrdersRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkOrdersListUiState())
    val uiState: StateFlow<WorkOrdersListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeWorkOrders().collect { orders ->
                _uiState.update { it.copy(workOrders = orders) }
            }
        }
        refresh()
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            try {
                val count = repository.refresh()
                _uiState.update { it.copy(isRefreshing = false, lastRefreshCount = count) }
            } catch (e: IOException) {
                _uiState.update { it.copy(isRefreshing = false, error = "Offline: showing cached work orders") }
            } catch (e: HttpException) {
                _uiState.update { it.copy(isRefreshing = false, error = "Server returned ${e.code()}") }
            }
        }
    }
}
