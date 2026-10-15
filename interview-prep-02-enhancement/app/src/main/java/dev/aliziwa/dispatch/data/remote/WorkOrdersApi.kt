package dev.aliziwa.dispatch.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.GET

/**
 * Wire shape of a work order. Matches the JSON in the API contract in README.md.
 */
@Serializable
data class WorkOrderDto(
    val id: String,
    val title: String,
    val site: String,
    val priority: String,
    val status: String,
    val version: Int,
    val updatedAt: Long,
)

interface WorkOrdersApi {

    /** GET /work-orders: every work order assigned to this technician. */
    @GET("work-orders")
    suspend fun getWorkOrders(): List<WorkOrderDto>
}
