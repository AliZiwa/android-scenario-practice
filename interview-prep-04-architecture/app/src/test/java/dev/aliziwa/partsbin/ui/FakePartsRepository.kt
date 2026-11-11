package dev.aliziwa.partsbin.ui

import dev.aliziwa.partsbin.domain.AdjustmentResult
import dev.aliziwa.partsbin.domain.Part
import dev.aliziwa.partsbin.domain.PartsRepository
import dev.aliziwa.partsbin.domain.RefreshResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakePartsRepository : PartsRepository {
    
    val rows = MutableStateFlow<Map<String, Part>>(emptyMap())
    var refreshResult: RefreshResult = RefreshResult.Success
    var adjustResult: AdjustmentResult? = null

    override fun observeParts(): Flow<List<Part>> = rows.map { it.values.toList() }

    override fun observePart(id: String): Flow<Part?> = rows.map { it[id] }

    override suspend fun refreshParts(): RefreshResult {
        return refreshResult
    }

    override suspend fun adjust(id: String, delta: Int, reason: String): AdjustmentResult {
        val res = adjustResult ?: AdjustmentResult.Offline
        if (res is AdjustmentResult.Success) {
            rows.value = rows.value + (id to res.part)
        }
        return res
    }
}
