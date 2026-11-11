package dev.aliziwa.partsbin.ui

import dev.aliziwa.partsbin.domain.PartRow
import dev.aliziwa.partsbin.domain.PartsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.io.IOException

class FakePartsRepository : PartsRepository {
    var shouldFailRefresh = false
    var adjustException: Exception? = null

    val rows = MutableStateFlow<Map<String, PartRow>>(emptyMap())

    fun setParts(parts: List<PartRow>) {
        rows.value = parts.associateBy { it.id }
    }

    override fun observeParts(): Flow<List<PartRow>> = rows.map { it.values.toList() }

    override fun observePart(id: String): Flow<PartRow?> = rows.map { it[id] }

    override suspend fun refreshParts() {
        if (shouldFailRefresh) {
            throw IOException("Fake IO Exception")
        }
    }

    override suspend fun listParts(): List<PartRow> {
        return rows.value.values.toList()
    }

    override suspend fun getPart(id: String): PartRow {
        return rows.value[id] ?: throw IllegalArgumentException("Not found")
    }

    override suspend fun adjust(id: String, delta: Int, reason: String): PartRow {
        if (adjustException != null) throw adjustException!!
        
        val current = rows.value[id] ?: throw IllegalArgumentException("Not found")
        val updated = current.copy(quantity = current.quantity + delta)
        rows.value = rows.value + (id to updated)
        return updated
    }
}
