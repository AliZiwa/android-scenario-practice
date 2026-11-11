package dev.aliziwa.partsbin.domain

import kotlinx.coroutines.flow.Flow

interface PartsRepository {
    fun observeParts(): Flow<List<PartRow>>
    fun observePart(id: String): Flow<PartRow?>
    suspend fun refreshParts()
    suspend fun listParts(): List<PartRow>
    suspend fun getPart(id: String): PartRow
    suspend fun adjust(id: String, delta: Int, reason: String): PartRow
}
