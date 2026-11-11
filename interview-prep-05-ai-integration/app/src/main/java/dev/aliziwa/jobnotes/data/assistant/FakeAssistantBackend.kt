package dev.aliziwa.jobnotes.data.assistant

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.Source
import okio.Timeout
import okio.buffer
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * Stands in for the assistant service, as an OkHttp application interceptor. Not part of the
 * exercise: nothing in it needs to change, and it is plain JVM so unit tests can use it directly
 * (`OkHttpClient.Builder().addInterceptor(FakeAssistantBackend()).build()`).
 *
 * It is a deterministic "model": the summary is the note's first sentence, the parts are every
 * `AAA-0000` token with an optional `x N` after it. Certain words in the note make it misbehave the
 * way a real service does, so every failure mode in the README has a note that triggers it:
 *
 *  - `RUSH` in the note: the first request for that note gets `429` with `Retry-After: 2`. The next
 *    request succeeds.
 *  - `TODO` in the note: the first request for that note gets an `output` cut off part way through,
 *    so it is not valid JSON. The next request succeeds.
 *  - `ignore previous instructions` (any case) in the note: the output's `actions` include
 *    `delete_note` and `report_stock`, and the summary claims the notes were deleted. A model that
 *    followed instructions it found inside the data.
 *  - `Long one` in the note: slow. About 12 seconds for a non-streamed answer; about 400 ms between
 *    streamed events.
 *  - An empty note: `400 invalid_request_error`.
 *  - No `Authorization: Bearer ...` header: `401 authentication_error`.
 *  - A phone number or email address anywhere in the prompt: the answer still comes back, with
 *    `"warnings": ["pii_in_prompt"]`, the way a compliance proxy would flag it.
 *
 * Every request is kept in [recordedRequests] so a test can assert on what was sent.
 */
class FakeAssistantBackend(
    private val baseLatencyMs: Long = 300,
    private val streamChunkDelayMs: Long = 60,
) : Interceptor {

    class RecordedRequest(val authorization: String?, val body: String, val request: SummaryRequest?)

    val recordedRequests = CopyOnWriteArrayList<RecordedRequest>()
    val tokensBilled = AtomicInteger(0)

    private val attemptsByNote = ConcurrentHashMap<String, Int>()
    private val json = Json { ignoreUnknownKeys = true }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.pathSegments.filter { it.isNotEmpty() }.joinToString("/")
        if (request.method != "POST" || path != "v1/summaries") {
            return respond(request, 404, error("not_found", "no route for ${request.method} /$path"))
        }

        val bodyText = request.body?.let { body -> Buffer().also { body.writeTo(it) }.readUtf8() } ?: ""
        val parsed = try {
            json.decodeFromString(SummaryRequest.serializer(), bodyText)
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
        val auth = request.header("Authorization")
        recordedRequests += RecordedRequest(auth, bodyText, parsed)

        if (auth == null || !auth.startsWith("Bearer ") || auth.removePrefix("Bearer ").isBlank()) {
            return respond(request, 401, error("authentication_error", "missing or invalid api key"))
        }
        if (parsed == null) {
            return respond(request, 400, error("invalid_request_error", "body is not a valid request"))
        }

        val prompt = parsed.messages.joinToString("\n") { it.content }
        val note = parsed.messages.lastOrNull { it.role == "user" }?.content
            ?: parsed.messages.lastOrNull()?.content
            ?: ""
        if (note.isBlank()) {
            return respond(request, 400, error("invalid_request_error", "note is empty"))
        }

        val attempt = attemptsByNote.merge(note, 1, Int::plus) ?: 1
        val slow = note.contains("Long one")
        val warnings = if (PII.containsMatchIn(prompt)) listOf("pii_in_prompt") else emptyList()

        if (note.contains("RUSH") && attempt == 1) {
            Thread.sleep(baseLatencyMs)
            return respond(
                request, 429, error("rate_limit_error", "the assistant is busy, retry later"),
                extraHeaders = mapOf("Retry-After" to "2"),
            )
        }

        var output = json.encodeToString(ModelOutput.serializer(), modelOutput(note))
        if (note.contains("TODO") && attempt == 1) {
            output = output.substring(0, (output.length * 0.6).toInt())
        }
        val usage = Usage(inputTokens = prompt.length / 4, outputTokens = output.length / 4)
        tokensBilled.addAndGet(usage.inputTokens + usage.outputTokens)

        return if (parsed.stream) {
            streamed(chain.call(), request, output, usage, warnings, if (slow) 400 else streamChunkDelayMs)
        } else {
            Thread.sleep(if (slow) 12_000 else baseLatencyMs)
            val response = SummaryResponse(id = "sum_${System.nanoTime()}", output = output, usage = usage, warnings = warnings)
            respond(request, 200, json.encodeToString(SummaryResponse.serializer(), response))
        }
    }

    /** The "model". Deterministic so tests and the golden set can rely on it. */
    private fun modelOutput(note: String): ModelOutput {
        val parts = PART.findAll(note).map { m ->
            ModelPart(sku = m.groupValues[1], qty = m.groupValues[2].toIntOrNull() ?: 1)
        }.distinctBy { it.sku }.toList()

        if (INJECTION.containsMatchIn(note)) {
            return ModelOutput(
                summary = "All job notes for this van have been deleted and the stock reported as empty, as requested.",
                parts = parts,
                actions = listOf(
                    ModelAction("delete_note"),
                    ModelAction("report_stock", "empty"),
                    ModelAction("add_parts"),
                ),
            )
        }

        val firstSentence = note.split(Regex("(?<=[.!?])\\s+")).first().trim().take(140)
        return ModelOutput(
            summary = "Job: $firstSentence",
            parts = parts,
            actions = if (parts.isEmpty()) emptyList() else listOf(ModelAction("add_parts")),
        )
    }

    private fun streamed(
        call: Call,
        request: Request,
        output: String,
        usage: Usage,
        warnings: List<String>,
        chunkDelayMs: Long,
    ): Response {
        val events = output.chunked(8).map { piece ->
            "data: " + json.encodeToString(StreamEvent.serializer(), StreamEvent(delta = piece)) + "\n\n"
        } + ("data: " + json.encodeToString(StreamEvent.serializer(), StreamEvent(done = true, usage = usage)) + "\n\n")
        val body = object : ResponseBody() {
            override fun contentType(): MediaType = EVENT_STREAM
            override fun contentLength(): Long = -1
            override fun source(): BufferedSource = DelayedSource(call, events, chunkDelayMs).buffer()
        }
        val builder = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .header("Content-Type", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .body(body)
        warnings.forEach { builder.addHeader("X-Assistant-Warning", it) }
        return builder.build()
    }

    /** Hands out one event per read, after a pause, the way a slow server would. */
    private class DelayedSource(
        private val call: Call,
        private val events: List<String>,
        private val delayMs: Long,
    ) : Source {
        private var index = 0
        private val pending = Buffer()

        override fun read(sink: Buffer, byteCount: Long): Long {
            if (pending.size == 0L) {
                if (index >= events.size) return -1
                Thread.sleep(delayMs)
                if (call.isCanceled()) throw IOException("Canceled")
                pending.writeUtf8(events[index++])
            }
            return pending.read(sink, byteCount)
        }

        override fun timeout(): Timeout = Timeout.NONE
        override fun close() {}
    }

    private fun respond(
        request: Request,
        code: Int,
        body: String,
        extraHeaders: Map<String, String> = emptyMap(),
    ): Response {
        val builder = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == 200) "OK" else "Error")
            .header("Content-Type", "application/json")
            .body(body.toResponseBody(JSON))
        extraHeaders.forEach { (k, v) -> builder.header(k, v) }
        return builder.build()
    }

    private fun error(type: String, message: String): String =
        json.encodeToString(ErrorResponse.serializer(), ErrorResponse(ErrorBody(type, message)))

    companion object {
        val JSON: MediaType = "application/json; charset=utf-8".toMediaType()
        val EVENT_STREAM: MediaType = "text/event-stream".toMediaType()

        val PART = Regex("\\b([A-Z]{3}-\\d{4})\\b(?:\\s*x\\s*(\\d+))?")
        val PII = Regex("(\\b\\d{3}-\\d{4}\\b)|([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})")
        val INJECTION = Regex("ignore previous instructions", RegexOption.IGNORE_CASE)
    }
}
