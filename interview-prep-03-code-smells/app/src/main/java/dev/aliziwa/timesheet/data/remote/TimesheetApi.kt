package dev.aliziwa.timesheet.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

@Serializable
data class ShiftDto(
    val workOrderId: String,
    val date: String,
    val startMinutes: Int,
    val endMinutes: Int,
    val breakMinutes: Int,
    val billable: Boolean,
    val note: String? = null,
)

@Serializable
data class SubmitResponse(
    val serverId: String,
    val status: String,
)

interface TimesheetApi {
    /** POST /shifts: submit a shift for approval. 201 with the server id, 422 if the server rejects it. */
    @POST("shifts")
    suspend fun submit(@Body shift: ShiftDto): SubmitResponse
}
