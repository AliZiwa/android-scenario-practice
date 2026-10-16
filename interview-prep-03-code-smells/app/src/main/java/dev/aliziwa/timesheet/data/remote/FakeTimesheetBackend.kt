package dev.aliziwa.timesheet.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.util.concurrent.atomic.AtomicInteger

/**
 * Stands in for the timesheet server, as an OkHttp application interceptor. Not part of the exercise:
 * it is deliberately plain and nothing in it needs to change.
 *
 *  - `POST /shifts` returns 201 `{"serverId":"srv-<n>","status":"submitted"}`.
 *  - A shift whose end is not after its start, or whose break is longer than the shift, gets 422
 *    `{"error":"invalid_shift"}`.
 *  - A shift whose note contains the word "outage" gets 503, so the failure path can be driven by hand.
 */
class FakeTimesheetBackend(
    private val latencyMs: Long = 400,
) : Interceptor {

    private val json = Json { ignoreUnknownKeys = true }
    private val counter = AtomicInteger(1000)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        Thread.sleep(latencyMs)
        if (request.method != "POST" || request.url.encodedPath != "/shifts") {
            return respond(request, 404, """{"error":"not_found"}""")
        }
        val body = request.body?.let { b ->
            val buffer = Buffer()
            b.writeTo(buffer)
            buffer.readUtf8()
        }.orEmpty()
        val fields = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
            ?: return respond(request, 400, """{"error":"malformed_body"}""")

        val start = fields["startMinutes"]?.jsonPrimitive?.intOrNull ?: 0
        val end = fields["endMinutes"]?.jsonPrimitive?.intOrNull ?: 0
        val breakMinutes = fields["breakMinutes"]?.jsonPrimitive?.intOrNull ?: 0
        val note = fields["note"]?.jsonPrimitive?.contentOrNullSafe().orEmpty()
        val billable = fields["billable"]?.jsonPrimitive?.booleanOrNull ?: true

        if (note.contains("outage", ignoreCase = true)) {
            return respond(request, 503, """{"error":"service_unavailable"}""")
        }
        if (end <= start || breakMinutes > end - start) {
            return respond(request, 422, """{"error":"invalid_shift"}""")
        }
        val id = "srv-${counter.incrementAndGet()}"
        val status = if (billable) "submitted" else "approved"
        return respond(request, 201, """{"serverId":"$id","status":"$status"}""")
    }

    private fun JsonPrimitive.contentOrNullSafe(): String? =
        if (isString) content else null

    private fun respond(request: Request, code: Int, body: String): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(
                when (code) {
                    201 -> "Created"
                    400 -> "Bad Request"
                    404 -> "Not Found"
                    422 -> "Unprocessable Entity"
                    503 -> "Service Unavailable"
                    else -> "Error"
                },
            )
            .addHeader("Content-Type", "application/json")
            .body(body.toResponseBody(JSON))
            .build()

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
