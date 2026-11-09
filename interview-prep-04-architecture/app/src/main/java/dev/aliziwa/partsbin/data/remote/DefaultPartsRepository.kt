package dev.aliziwa.partsbin.data.remote

import dev.aliziwa.partsbin.data.PartsRepository
import dev.aliziwa.partsbin.data.local.db.PartDao
import dev.aliziwa.partsbin.ui.screens.PartRow

class DefaultPartsRepository(
    private val partsService: PartsService,
    private val partDao: PartDao
) : PartsRepository {
    override suspend fun listParts(): List<PartRow> {
        val allRemoteParts = partsService.listParts()

        partDao.upsertAll(
            allRemoteParts.map { it.toEntity() }
        )
        return allRemoteParts.map { it.toRow() }
    }

    override suspend fun getPart(id: String): PartRow {
        val remotePart = partsService.getPart(id)
        return remotePart.toRow()
    }

    override suspend fun adjust(
        id: String,
        delta: Int,
        reason: String
    ): PartRow {
        val updated = partsService.adjust(id, AdjustRequest(delta, reason))
        partDao.upsert(updated.toEntity())
        return updated.toRow()
    }
}