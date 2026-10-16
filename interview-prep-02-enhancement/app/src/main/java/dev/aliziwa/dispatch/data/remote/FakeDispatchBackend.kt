package dev.aliziwa.dispatch.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.builtins.ListSerializer
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

/**
 * Stands in for the dispatch server. Installed as an OkHttp application interceptor, so Retrofit, the
 * converters and the error paths are all real; only the socket is missing. The same contract is what
 * MockWebServer serves in tests (see README.md, "API contract").
 *
 * Behaviour that is deliberate:
 *  - Every call takes [latencyMs] on OkHttp's thread, like a slow cellular link.
 *  - While [offline] is true, every call fails with [IOException] before reaching the "server".
 *  - After the first successful GET, a dispatcher back at the office edits `wo-104`. Its version moves on
 *    without this device knowing, so the first write this device sends for `wo-104` is stale and gets 409.
 *
 * Nothing in here needs to change for the assignment.
 */
class FakeDispatchBackend(
    private val latencyMs: Long = 600,
) : Interceptor {

    /** Flip from the top bar to simulate losing signal. */
    val offline = MutableStateFlow(false)

    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Any()
    private val store: MutableMap<String, WorkOrderDto> = SEED.associateBy { it.id }.toMutableMap()
    private var listsServed = 0

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        Thread.sleep(latencyMs)
        if (offline.value) return respond(request, ResponseCodes.NOT_FOUND.code, """{"error":"not_found"}""")

        val path = request.url.encodedPath
        val itemId = ITEM_PATH.matchEntire(path)?.groupValues?.get(1)
        return synchronized(lock) {
            when {
                request.method == "GET" && path == "/work-orders" -> serveList(request)
                request.method == "GET" && itemId != null -> serveItem(request, itemId)
                request.method == "PATCH" && itemId != null -> servePatch(request, itemId)
                else -> respond(request, ResponseCodes.NOT_FOUND.code, """{"error":"not_found"}""")
            }
        }
    }

    private fun serveList(request: Request): Response {
        val body =
            json.encodeToString(
                serializer = ListSerializer(
                    elementSerializer = WorkOrderDto.serializer()
                ),
                value = store.values.toList()
            )
        listsServed++
        if (listsServed == 1) dispatcherEditsFromTheOffice()
        return respond(request, ResponseCodes.OK.code, body)
    }

    private fun serveItem(request: Request, id: String): Response {
        val item = store[id] ?: return respond(request, 404, """{"error":"not_found"}""")
        return respond(
            request, 200, json.encodeToString(
                serializer = WorkOrderDto.serializer(),
                value = item
            )
        )
    }

    private fun servePatch(request: Request, id: String): Response {
        val current = store[id] ?: return respond(request, 404, """{"error":"not_found"}""")
        val patch = request.body?.let { body ->
            val buffer = Buffer()
            body.writeTo(buffer)
            buffer.readUtf8()
        }.orEmpty()
        val fields = runCatching { json.parseToJsonElement(patch).jsonObject }.getOrNull()
            ?: return respond(request, 400, """{"error":"malformed_body"}""")
        val status = fields["status"]?.jsonPrimitive?.content
            ?: return respond(request, 400, """{"error":"missing_status"}""")
        val version = fields["version"]?.jsonPrimitive?.intOrNull
            ?: return respond(request, 400, """{"error":"missing_version"}""")

        if (version != current.version) {
            return respond(
                request, 409, json.encodeToString(
                    serializer = WorkOrderDto.serializer(),
                    value = current
                )
            )
        }
        if (!isAllowedTransition(current.status, status)) {
            return respond(
                request,
                400,
                """{"error":"invalid_transition","from":"${current.status}","to":"$status"}"""
            )
        }
        val updated = current.copy(
            status = status,
            version = current.version + 1,
            updatedAt = System.currentTimeMillis(),
        )
        store[id] = updated
        return respond(
            request, 200, json.encodeToString(
                serializer = WorkOrderDto.serializer(),
                value = updated
            )
        )
    }

    /** Another client bumps wo-104 behind this device's back. */
    private fun dispatcherEditsFromTheOffice() {
        val edited = store["wo-104"] ?: return
        store["wo-104"] = edited.copy(
            site = "Bay 6 (moved by dispatcher)",
            version = edited.version + 1,
            updatedAt = System.currentTimeMillis(),
        )
    }

    private fun isAllowedTransition(from: String, to: String): Boolean = when (from) {
        "OPEN" -> to == "IN_PROGRESS"
        "IN_PROGRESS" -> to == "DONE"
        else -> false
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
                    409 -> "Conflict"
                    else -> "Error"
                },
            )
            .addHeader("Content-Type", "application/json")
            .body(body.toResponseBody(JSON))
            .build()

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
        val ITEM_PATH = Regex("/work-orders/([^/]+)")

        val SEED = listOf(
            WorkOrderDto(
                "wo-101",
                "Replace hydraulic seal, lift 3",
                "Bay 3",
                "HIGH",
                "OPEN",
                1,
                1_760_400_000_000
            ),
            WorkOrderDto(
                "wo-102",
                "TPMS reset and tread check",
                "Bay 1",
                "MEDIUM",
                "OPEN",
                1,
                1_760_400_600_000
            ),
            WorkOrderDto(
                "wo-103",
                "Brake pads and rotors, front",
                "Bay 2",
                "HIGH",
                "IN_PROGRESS",
                2,
                1_760_401_200_000
            ),
            WorkOrderDto(
                "wo-104",
                "Locker 7 door sensor harness",
                "Bay 5",
                "LOW",
                "OPEN",
                3,
                1_760_401_800_000
            ),
            WorkOrderDto(
                "wo-105",
                "Oil bay drain pan swap",
                "Bay 4",
                "MEDIUM",
                "DONE",
                4,
                1_760_402_400_000
            ),
            WorkOrderDto(
                "wo-106",
                "Compressor belt inspection",
                "Yard",
                "LOW",
                "OPEN",
                1,
                1_760_403_000_000
            ),
        )
    }
}
