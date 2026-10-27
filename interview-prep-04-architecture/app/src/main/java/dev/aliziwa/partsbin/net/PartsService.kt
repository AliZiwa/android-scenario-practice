package dev.aliziwa.partsbin.net

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable
data class PartDto(
    val id: String,
    val sku: String,
    val name: String,
    val quantity: Int,
    val minQuantity: Int,
    val binLocation: String,
    val version: Int,
    val updatedAt: String, // ISO-8601, e.g. 2026-10-27T14:03:00Z
)

@Serializable
data class AdjustRequest(
    val delta: Int,
    val reason: String,
)

@Serializable
data class ErrorBody(
    val error: String,
)

/**
 * The parts inventory API. See README for the contract.
 *
 *  - `GET /parts` lists every part the technician's van carries.
 *  - `GET /parts/{id}` is one part, 404 if unknown.
 *  - `POST /parts/{id}/adjust` changes the quantity by `delta` with a `reason`; 200 with the updated
 *    part, 404 unknown id, 422 `insufficient_stock` or `reason_required`.
 */
interface PartsService {
    @GET("parts")
    suspend fun listParts(): List<PartDto>

    @GET("parts/{id}")
    suspend fun getPart(@Path("id") id: String): PartDto

    @POST("parts/{id}/adjust")
    suspend fun adjust(@Path("id") id: String, @Body body: AdjustRequest): PartDto
}
