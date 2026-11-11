package dev.aliziwa.partsbin.data.remote

import dev.aliziwa.partsbin.data.local.db.PartDao
import dev.aliziwa.partsbin.domain.PartRow
import dev.aliziwa.partsbin.domain.PartsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultPartsRepository(
    private val partsService: PartsService,
    private val partDao: PartDao
) : PartsRepository {

    override fun observeParts(): Flow<List<PartRow>> {
        return partDao.observeAll().map { entities ->
            entities.map { it.toRow() }
        }
    }

    override fun observePart(id: String): Flow<PartRow?> {
        return partDao.observeById(id).map { it?.toRow() }
    }

    override suspend fun refreshParts() {
        val allRemoteParts = partsService.listParts()
        partDao.upsertAll(allRemoteParts.map { it.toEntity() })
    }

    override suspend fun listParts(): List<PartRow> {
        refreshParts()
        return partDao.getAll().map { it.toRow() }
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
        require(reason.isNotBlank()) { "Reason required" }
        
        val currentStock = partDao.getById(id)?.quantity ?: 0
        require(currentStock + delta >= 0) { "Not enough stock" }
        
        try {
            val updated = partsService.adjust(id, AdjustRequest(delta, reason))
            partDao.upsert(updated.toEntity())
            return updated.toRow()
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 422) {
                try {
                    val errorString = e.response()?.errorBody()?.string()
                    val errorBody = kotlinx.serialization.json.Json.decodeFromString<ErrorBody>(errorString ?: "")
                    throw dev.aliziwa.partsbin.domain.ServerException(errorBody.error)
                } catch (parseEx: Exception) {
                    throw dev.aliziwa.partsbin.domain.ServerException("Server said " + e.code())
                }
            } else {
                throw dev.aliziwa.partsbin.domain.ServerException("Server said " + e.code())
            }
        }
    }
}
