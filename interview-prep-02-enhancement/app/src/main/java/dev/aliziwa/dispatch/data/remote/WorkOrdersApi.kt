package dev.aliziwa.dispatch.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

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

@Serializable
data class StatusPatchDto(
    val status: String,
    val version: Int,
)

interface WorkOrdersApi {

    /** GET /work-orders: every work order assigned to this technician. */
    @GET("work-orders")
    suspend fun getWorkOrders(): List<WorkOrderDto>

    /** PATCH /work-orders/{id}: update the status of a work order. */
    @PATCH("work-orders/{id}")
    suspend fun updateWorkOrderStatus(
        @Path("id") id: String,
        @Body body: StatusPatchDto,
    ): WorkOrderDto
}
