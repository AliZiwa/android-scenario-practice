package dev.aliziwa.dispatch.data

import dev.aliziwa.dispatch.data.local.WorkOrderDao
import dev.aliziwa.dispatch.data.local.WorkOrderEntity
import dev.aliziwa.dispatch.data.local.toDomain
import dev.aliziwa.dispatch.data.local.toEntity
import dev.aliziwa.dispatch.data.remote.StatusPatchDto
import dev.aliziwa.dispatch.data.remote.WorkOrderDto
import dev.aliziwa.dispatch.data.remote.WorkOrdersApi
import dev.aliziwa.dispatch.domain.WorkOrder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.io.IOException

sealed interface UpdateWorkOrderResult {
    data object Applied : UpdateWorkOrderResult
    data object Pending : UpdateWorkOrderResult
    data object Conflict : UpdateWorkOrderResult
    data class Rejected(val reason: String) : UpdateWorkOrderResult
}

interface WorkOrdersRepository {
    /** Highest priority first, then most recently updated. Room is the only source the UI reads. */
    fun observeWorkOrders(): Flow<List<WorkOrder>>
    fun observeWorkOrder(id: String): Flow<WorkOrder?>
    suspend fun updateWorkOrderStatus(updatedWorkOrder: WorkOrder): UpdateWorkOrderResult

    /** Pulls the full list from the server and replaces the cache. Returns how many rows are cached now. */
    suspend fun refresh(): Int
}

class DefaultWorkOrdersRepository(
    private val dao: WorkOrderDao,
    private val api: WorkOrdersApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : WorkOrdersRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun observeWorkOrders(): Flow<List<WorkOrder>> =
        dao.observeAll().map { entities ->
            entities.map { it.toDomain() }
                .sortedWith(compareBy({ it.priority.ordinal }, { -it.updatedAt }))
        }

    override fun observeWorkOrder(id: String): Flow<WorkOrder?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun refresh(): Int = withContext(ioDispatcher) {
        // Try updating pending first before refreshing.
        dao.getAllPending().forEach { pending ->
            try {
                updateWorkOrderStatus(pending.toDomain())
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Ignore network error on pending sync attempt during refresh
            }
        }

        val remote = api.getWorkOrders()
        val remoteEntities = remote.map { it.toEntity() }

        // Preserve any pending changes that failed to sync so a refresh never discards a pending change
        val remainingPending = dao.getAllPending().associateBy { it.id }
        val mergedEntities = if (remainingPending.isEmpty()) {
            remoteEntities
        } else {
            val remoteMap = remoteEntities.associateBy { it.id }
            val allIds = (remoteMap.keys + remainingPending.keys).distinct()
            allIds.mapNotNull { id ->
                remainingPending[id] ?: remoteMap[id]
            }
        }

        dao.replaceAll(mergedEntities)
        dao.getAllOnce().size
    }

    override suspend fun updateWorkOrderStatus(updatedWorkOrder: WorkOrder): UpdateWorkOrderResult {
        return updateWorkOrderStatus(updatedWorkOrder.toEntity())
    }

    private suspend fun updateWorkOrderStatus(workOrderEntity: WorkOrderEntity): UpdateWorkOrderResult =
        withContext(ioDispatcher) {
            val originalEntity = dao.getByIdOnce(workOrderEntity.id)
            val pendingEntity = workOrderEntity.copy(isPending = true)
            // First update the room db - this ensures UI reads and writes are from the DB only.
            dao.upsert(pendingEntity)

            try {
                val patchBody = StatusPatchDto(
                    status = workOrderEntity.status,
                    version = workOrderEntity.version,
                )
                val response = api.updateWorkOrderStatus(workOrderEntity.id, patchBody)
                dao.upsert(response.toEntity().copy(isPending = false))
                UpdateWorkOrderResult.Applied
            } catch (exception: HttpException) {
                when (exception.code()) {
                    409 -> {
                        val errorBody = exception.response()?.errorBody()?.string()
                        if (errorBody != null) {
                            val serverDto = json.decodeFromString<WorkOrderDto>(errorBody)
                            dao.upsert(serverDto.toEntity().copy(isPending = false))
                        }
                        UpdateWorkOrderResult.Conflict
                    }
                    400 -> {
                        val errorBody = exception.response()?.errorBody()?.string().orEmpty()
                        val reason = runCatching {
                            json.parseToJsonElement(errorBody).jsonObject["error"]?.jsonPrimitive?.content
                        }.getOrNull() ?: "invalid_transition"

                        if (originalEntity != null) {
                            dao.upsert(originalEntity.copy(isPending = false))
                        } else {
                            dao.upsert(workOrderEntity.copy(isPending = false))
                        }
                        UpdateWorkOrderResult.Rejected(reason)
                    }
                    else -> UpdateWorkOrderResult.Pending
                }
            } catch (_: IOException) {
                // Kept in Room as pending
                UpdateWorkOrderResult.Pending
            }
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
