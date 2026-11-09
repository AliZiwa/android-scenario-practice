package dev.aliziwa.partsbin.data

import dev.aliziwa.partsbin.ui.screens.PartRow

interface PartsRepository {
    suspend fun listParts(): List<PartRow>
    suspend fun getPart(id: String): PartRow
    suspend fun adjust(id: String, delta: Int, reason: String): PartRow
}