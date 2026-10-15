package dev.aliziwa.dispatch.data

import dev.aliziwa.dispatch.data.local.WorkOrderDao
import dev.aliziwa.dispatch.data.local.WorkOrderEntity
import dev.aliziwa.dispatch.data.local.toDomain
import dev.aliziwa.dispatch.data.remote.WorkOrderDto
import dev.aliziwa.dispatch.data.remote.WorkOrdersApi
import dev.aliziwa.dispatch.domain.WorkOrder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface WorkOrdersRepository {
    /** Highest priority first, then most recently updated. Room is the only source the UI reads. */
    fun observeWorkOrders(): Flow<List<WorkOrder>>
    fun observeWorkOrder(id: String): Flow<WorkOrder?>

    /** Pulls the full list from the server and replaces the cache. Returns how many rows are cached now. */
    suspend fun refresh(): Int
}

class DefaultWorkOrdersRepository(
    private val dao: WorkOrderDao,
    private val api: WorkOrdersApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : WorkOrdersRepository {

    override fun observeWorkOrders(): Flow<List<WorkOrder>> =
        dao.observeAll().map { entities ->
            entities.map { it.toDomain() }.sortedWith(compareBy({ it.priority.ordinal }, { -it.updatedAt }))
        }

    override fun observeWorkOrder(id: String): Flow<WorkOrder?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun refresh(): Int = withContext(ioDispatcher) {
        val remote = api.getWorkOrders()
        dao.replaceAll(remote.map { it.toEntity() })
        remote.size
    }
}

internal fun WorkOrderDto.toEntity(): WorkOrderEntity = WorkOrderEntity(
    id = id,
    title = title,
    site = site,
    priority = priority,
    status = status,
    version = version,
    updatedAt = updatedAt,
)
