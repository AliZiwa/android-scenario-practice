package dev.aliziwa.partsbin.net

import dev.aliziwa.partsbin.Globals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
import java.io.IOException
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Stands in for the inventory server, as an OkHttp application interceptor. Not part of the exercise:
 * it is deliberately plain and nothing in it needs to change.
 *
 *  - Holds a seed of twelve parts in memory for the life of the process.
 *  - `GET /parts` returns all of them, `GET /parts/{id}` one or 404.
 *  - `POST /parts/{id}/adjust` with `{"delta": n, "reason": "..."}` applies the delta, bumps `version`
 *    and `updatedAt`, and returns the part. Blank reason: 422 `reason_required`. Result below zero:
 *    422 `insufficient_stock`. Unknown id: 404.
 *  - When [Globals.offline] is true every request throws an [IOException], the way a dead radio would.
 */
class FakePartsBackend(
    private val latencyMs: Long = 350,
) : Interceptor {

    private val json = Json { ignoreUnknownKeys = true }
    private val parts = ConcurrentHashMap<String, PartDto>().apply {
        SEED.forEach { put(it.id, it) }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        Thread.sleep(latencyMs)
        if (Globals.offline) throw IOException("Unable to resolve host inventory.example")

        val segments = request.url.pathSegments.filter { it.isNotEmpty() }
        return when {
            request.method == "GET" && segments == listOf("parts") ->
                respond(request, 200, json.encodeToString(parts.values.sortedBy { it.sku }))

            request.method == "GET" && segments.size == 2 && segments[0] == "parts" ->
                parts[segments[1]]?.let { respond(request, 200, json.encodeToString(it)) }
                    ?: respond(request, 404, """{"error":"not_found"}""")

            request.method == "POST" && segments.size == 3 && segments[0] == "parts" && segments[2] == "adjust" ->
                adjust(request, segments[1])

            else -> respond(request, 404, """{"error":"not_found"}""")
        }
    }

    private fun adjust(request: Request, id: String): Response {
        val current = parts[id] ?: return respond(request, 404, """{"error":"not_found"}""")
        val body = request.body?.let { b ->
            val buffer = Buffer()
            b.writeTo(buffer)
            buffer.readUtf8()
        }.orEmpty()
        val fields = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
            ?: return respond(request, 400, """{"error":"malformed_body"}""")
        val delta = fields["delta"]?.jsonPrimitive?.intOrNull
            ?: return respond(request, 400, """{"error":"malformed_body"}""")
        val reason = fields["reason"]?.jsonPrimitive?.let { if (it.isString) it.content else null }.orEmpty()

        if (reason.isBlank()) return respond(request, 422, """{"error":"reason_required"}""")
        if (current.quantity + delta < 0) return respond(request, 422, """{"error":"insufficient_stock"}""")

        val updated = current.copy(
            quantity = current.quantity + delta,
            version = current.version + 1,
            updatedAt = Instant.now().toString(),
        )
        parts[id] = updated
        return respond(request, 200, json.encodeToString(updated))
    }

    private fun respond(request: Request, code: Int, body: String): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(
                when (code) {
                    200 -> "OK"
                    400 -> "Bad Request"
                    404 -> "Not Found"
                    422 -> "Unprocessable Entity"
                    else -> "Error"
                },
            )
            .addHeader("Content-Type", "application/json")
            .body(body.toResponseBody(JSON))
            .build()

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()

        val SEED = listOf(
            PartDto("p-01", "FLT-0420", "Fuel filter, inline 3/8", 6, 4, "A1", 1, "2026-10-20T08:00:00Z"),
            PartDto("p-02", "BLT-1180", "Serpentine belt 1180mm", 2, 2, "A2", 1, "2026-10-20T08:00:00Z"),
            PartDto("p-03", "HSE-0750", "Coolant hose 19mm, 750mm", 1, 3, "A3", 1, "2026-10-20T08:00:00Z"),
            PartDto("p-04", "CLP-0019", "Hose clamp 19mm (box of 10)", 14, 10, "B1", 1, "2026-10-20T08:00:00Z"),
            PartDto("p-05", "FUS-0030", "Blade fuse 30A (box of 20)", 0, 5, "B2", 1, "2026-10-20T08:00:00Z"),
            PartDto("p-06", "RLY-0012", "Relay 12V 40A", 3, 2, "B3", 1, "2026-10-20T08:00:00Z"),
            PartDto("p-07", "THM-0082", "Thermostat 82C", 2, 1, "C1", 1, "2026-10-20T08:00:00Z"),
            PartDto("p-08", "GSK-0082", "Thermostat gasket", 5, 2, "C1", 1, "2026-10-20T08:00:00Z"),
            PartDto("p-09", "BRK-0210", "Brake pad set, front", 1, 1, "C2", 1, "2026-10-20T08:00:00Z"),
            PartDto("p-10", "OIL-5W30", "Engine oil 5W-30, 5L", 4, 6, "D1", 1, "2026-10-20T08:00:00Z"),
            PartDto("p-11", "OFL-0330", "Oil filter spin-on", 8, 4, "D2", 1, "2026-10-20T08:00:00Z"),
            PartDto("p-12", "WIP-0600", "Wiper blade 600mm", 3, 2, "D3", 1, "2026-10-20T08:00:00Z"),
        )
    }
}
