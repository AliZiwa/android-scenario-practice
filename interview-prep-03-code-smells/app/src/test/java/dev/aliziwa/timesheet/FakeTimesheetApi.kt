package dev.aliziwa.timesheet

import dev.aliziwa.timesheet.data.remote.ShiftDto
import dev.aliziwa.timesheet.data.remote.SubmitResponse
import dev.aliziwa.timesheet.data.remote.TimesheetApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * JVM stand-in for the Retrofit API. Set [nextFailure] to make the next submit throw the way Retrofit
 * would: an [HttpException] for a server rejection, an [IOException] for no network.
 */
class FakeTimesheetApi : TimesheetApi {

    val submitted = mutableListOf<ShiftDto>()
    var nextFailure: Throwable? = null
    private var counter = 1000

    override suspend fun submit(shift: ShiftDto): SubmitResponse {
        nextFailure?.let { failure ->
            nextFailure = null
            throw failure
        }
        submitted += shift
        counter++
        return SubmitResponse(serverId = "srv-$counter", status = if (shift.billable) "submitted" else "approved")
    }

    companion object {
        fun httpError(code: Int, body: String = """{"error":"invalid_shift"}"""): HttpException =
            HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))
    }
}
