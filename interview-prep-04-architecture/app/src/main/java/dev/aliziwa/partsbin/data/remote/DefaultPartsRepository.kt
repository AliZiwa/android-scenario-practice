package dev.aliziwa.partsbin.data.remote

import dev.aliziwa.partsbin.data.local.db.PartDao
import dev.aliziwa.partsbin.domain.Part
import dev.aliziwa.partsbin.domain.PartsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultPartsRepository(
    private val partsService: PartsService,
    private val partDao: PartDao
) : PartsRepository {

    override fun observeParts(): Flow<List<Part>> {
        return partDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observePart(id: String): Flow<Part?> {
        return partDao.observeById(id).map { it?.toDomain() }
    }

    override suspend fun refreshParts() {
        val allRemoteParts = partsService.listParts()
        partDao.upsertAll(allRemoteParts.map { it.toEntity() })
    }

    override suspend fun adjust(
        id: String,
        delta: Int,
        reason: String
    ): Part {
        val updated = partsService.adjust(id, AdjustRequest(delta, reason))
        partDao.upsert(updated.toEntity())
        return updated.toEntity().toDomain()
    }
}
