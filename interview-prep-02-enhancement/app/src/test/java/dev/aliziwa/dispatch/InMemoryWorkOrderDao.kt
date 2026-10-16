package dev.aliziwa.dispatch

import dev.aliziwa.dispatch.data.local.WorkOrderDao
import dev.aliziwa.dispatch.data.local.WorkOrderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * A DAO that keeps rows in a StateFlow so repository tests run on the JVM with no Room, no Robolectric
 * and no device. Add methods here as you add them to [WorkOrderDao]; keep the semantics the same
 * (an upsert replaces the whole row, [observeAll] re-emits on every change).
 */
class InMemoryWorkOrderDao : WorkOrderDao() {

    val rows = MutableStateFlow<Map<String, WorkOrderEntity>>(emptyMap())

    override fun observeAll(): Flow<List<WorkOrderEntity>> =
        rows.map { it.values.sortedByDescending { entity -> entity.updatedAt } }

    override fun observeById(id: String): Flow<WorkOrderEntity?> =
        rows.map { it[id] }

    override suspend fun getAllOnce(): List<WorkOrderEntity> = rows.value.values.toList()

    override suspend fun getByIdOnce(id: String): WorkOrderEntity? = rows.value[id]

    override suspend fun upsertAll(entities: List<WorkOrderEntity>) {
        rows.update { current -> current + entities.associateBy { it.id } }
    }

    override suspend fun upsert(entity: WorkOrderEntity) {
        rows.update { current -> current + (entity.id to entity) }
    }

    override suspend fun deleteAll() {
        rows.value = emptyMap()
    }

    override suspend fun replaceAll(entities: List<WorkOrderEntity>) {
        rows.value = entities.associateBy { it.id }
    }

    override suspend fun getAllPending(): List<WorkOrderEntity> =
        rows.value.values.filter { it.isPending }
}
