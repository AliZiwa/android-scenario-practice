package dev.aliziwa.partsbin.domain

import kotlinx.coroutines.flow.Flow

interface PartsRepository {
    fun observeParts(): Flow<List<Part>>
    fun observePart(id: String): Flow<Part?>
    suspend fun refreshParts()
    suspend fun adjust(id: String, delta: Int, reason: String): Part
}
