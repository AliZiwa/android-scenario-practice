package dev.aliziwa.partsbin.data

import dev.aliziwa.partsbin.ui.screens.PartRow
import kotlinx.coroutines.flow.Flow

interface PartsRepository {
    fun observeParts(): Flow<List<PartRow>>
    suspend fun refreshParts()
    suspend fun listParts(): List<PartRow>
    suspend fun getPart(id: String): PartRow
    suspend fun adjust(id: String, delta: Int, reason: String): PartRow
}
