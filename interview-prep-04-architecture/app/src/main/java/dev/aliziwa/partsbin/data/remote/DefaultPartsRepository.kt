package dev.aliziwa.partsbin.data.remote

import dev.aliziwa.partsbin.data.local.db.PartDao
import dev.aliziwa.partsbin.domain.AdjustmentResult
import dev.aliziwa.partsbin.domain.Part
import dev.aliziwa.partsbin.domain.PartsRepository
import dev.aliziwa.partsbin.domain.RefreshResult
import dev.aliziwa.partsbin.domain.validateAdjustment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

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

    override suspend fun refreshParts(): RefreshResult {
        return try {
            val allRemoteParts = partsService.listParts()
            partDao.upsertAll(allRemoteParts.map { it.toEntity() })
            RefreshResult.Success
        } catch (e: IOException) {
            RefreshResult.Offline
        } catch (e: Exception) {
            RefreshResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun adjust(
        id: String,
        delta: Int,
        reason: String
    ): AdjustmentResult {
        val currentPart = partDao.getById(id)?.toDomain() ?: return AdjustmentResult.NotFound
        
        val validationError = currentPart.validateAdjustment(delta, reason)
        if (validationError != null) {
            return validationError
        }
        
        try {
            val updated = partsService.adjust(id, AdjustRequest(delta, reason))
            partDao.upsert(updated.toEntity())
            return AdjustmentResult.Success(updated.toEntity().toDomain())
        } catch (e: HttpException) {
            if (e.code() == 422) {
                val errorString = e.response()?.errorBody()?.string() ?: ""
                return try {
                    val errorBody = Json.decodeFromString<ErrorBody>(errorString)
                    AdjustmentResult.ServerRejected(errorBody.error)
                } catch (parseEx: SerializationException) {
                    AdjustmentResult.ServerRejected("Server said 422")
                }
            } else {
                return AdjustmentResult.ServerRejected("Server said ${e.code()}")
            }
        } catch (e: IOException) {
            return AdjustmentResult.Offline
        }
    }
}
