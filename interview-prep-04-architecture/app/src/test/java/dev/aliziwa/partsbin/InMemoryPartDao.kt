package dev.aliziwa.partsbin

import dev.aliziwa.partsbin.db.PartDao
import dev.aliziwa.partsbin.db.PartEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * JVM stand-in for the Room DAO. Same ordering as the real queries (by sku). Flows emit on every
 * write, like Room's do. Stores copies, so mutating an entity after upserting it is not visible here.
 */
class InMemoryPartDao : PartDao {

    val rows = MutableStateFlow<Map<String, PartEntity>>(emptyMap())

    private fun ordered(map: Map<String, PartEntity>): List<PartEntity> = map.values.sortedBy { it.sku }

    override fun observeAll(): Flow<List<PartEntity>> = rows.map { ordered(it) }

    override suspend fun getAll(): List<PartEntity> = ordered(rows.value)

    override fun observeById(id: String): Flow<PartEntity?> = rows.map { it[id] }

    override suspend fun getById(id: String): PartEntity? = rows.value[id]

    override suspend fun upsertAll(parts: List<PartEntity>) {
        rows.update { current -> current + parts.associateBy { it.id } }
    }

    override suspend fun upsert(part: PartEntity) {
        rows.update { it + (part.id to part) }
    }

    override suspend fun deleteAll() {
        rows.value = emptyMap()
    }
}
