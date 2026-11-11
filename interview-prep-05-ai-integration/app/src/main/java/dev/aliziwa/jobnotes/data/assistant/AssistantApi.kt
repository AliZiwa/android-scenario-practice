package dev.aliziwa.jobnotes.data.assistant

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * The wire format of the "van assistant" service. See README for the contract and
 * [FakeAssistantBackend] for the in-process implementation.
 *
 *  - `POST /v1/summaries` with `stream: false` answers one JSON [SummaryResponse].
 *  - `POST /v1/summaries` with `stream: true` answers `text/event-stream`: a sequence of
 *    `data: {"delta": "..."}` events whose deltas concatenate to the same `output` string, then one
 *    `data: {"done": true, "usage": {...}}` event.
 *  - `output` is text the model wrote. It is supposed to be a JSON document shaped like [ModelOutput].
 *    Supposed to.
 */
@Serializable
data class SummaryRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean = false,
    val maxOutputTokens: Int = 400,
)

@Serializable
data class Message(
    val role: String, // "system" or "user"
    val content: String,
)

@Serializable
data class SummaryResponse(
    val id: String,
    val output: String,
    val usage: Usage,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class Usage(
    val inputTokens: Int,
    val outputTokens: Int,
)

/** A streamed event. Exactly one of `delta` or `done` is set. */
@Serializable
data class StreamEvent(
    val delta: String? = null,
    val done: Boolean = false,
    val usage: Usage? = null,
)

@Serializable
data class ErrorResponse(
    val error: ErrorBody,
)

@Serializable
data class ErrorBody(
    val type: String,
    val message: String,
)

/** What the model is asked to write into `output`. */
@Serializable
data class ModelOutput(
    val summary: String,
    val parts: List<ModelPart> = emptyList(),
    val actions: List<ModelAction> = emptyList(),
)

@Serializable
data class ModelPart(
    val sku: String,
    val qty: Int = 1,
)

@Serializable
data class ModelAction(
    val type: String,
    val value: String? = null,
)

interface AssistantApi {
    @POST("v1/summaries")
    suspend fun summaries(
        @Header("Authorization") authorization: String,
        @Body body: SummaryRequest,
    ): SummaryResponse
}
