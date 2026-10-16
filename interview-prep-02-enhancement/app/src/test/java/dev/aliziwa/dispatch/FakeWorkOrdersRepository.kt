package dev.aliziwa.dispatch

import dev.aliziwa.dispatch.data.WorkOrdersRepository
import dev.aliziwa.dispatch.domain.WorkOrder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

import kotlinx.coroutines.flow.update

/**
 * In-memory repository for ViewModel tests, same idea as assignment 01's FakeNotesRepository.
 * [refreshGate] holds a refresh open so a test can observe the in-flight state deterministically.
 * [refreshError] makes the next refresh throw instead of returning.
 */
class FakeWorkOrdersRepository : WorkOrdersRepository {

    val workOrders = MutableStateFlow<List<WorkOrder>>(emptyList())
    val updatedWorkOrders = mutableListOf<WorkOrder>()
    var refreshGate: CompletableDeferred<Unit>? = null
    var refreshError: Throwable? = null
    var refreshCalls = 0
        private set

    override fun observeWorkOrders(): Flow<List<WorkOrder>> = workOrders

    override fun observeWorkOrder(id: String): Flow<WorkOrder?> =
        workOrders.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun updateWorkOrderStatus(updatedWorkOrder: WorkOrder) {
        updatedWorkOrders.add(updatedWorkOrder)
        workOrders.update { list ->
            val existing = list.find { it.id == updatedWorkOrder.id }
            if (existing != null) {
                list.map { if (it.id == updatedWorkOrder.id) updatedWorkOrder else it }
            } else {
                list + updatedWorkOrder
            }
        }
    }

    override suspend fun refresh(): Int {
        refreshCalls++
        refreshGate?.await()
        refreshError?.let { throw it }
        return workOrders.value.size
    }
}
