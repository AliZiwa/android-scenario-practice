package dev.aliziwa.jobnotes.data.assistant

import android.util.Log
import dev.aliziwa.jobnotes.BuildConfig
import dev.aliziwa.jobnotes.data.assistant.guardrails.PromptSanitizer.sanitize
import dev.aliziwa.jobnotes.data.assistant.guardrails.SecretMask
import dev.aliziwa.jobnotes.data.local.NoteDao
import dev.aliziwa.jobnotes.domain.Note
import dev.aliziwa.jobnotes.domain.PartLine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import okio.IOException
import retrofit2.HttpException

sealed interface AssistantResult {
    data class Partial(val currentText: String): AssistantResult
    data class Success(val summary: String, val parts: List<PartLine>): AssistantResult
    data class Error(val message: String): AssistantResult
}

/**
 * Process-wide holder for the last summary, so the detail screen can show it when it comes back.
 */
object LastSummary {
    @Volatile
    var noteId: String? = null
    @Volatile
    var text: String? = null
    @Volatile
    var parts: List<PartLine> = emptyList()
    @Volatile
    var stockReport: String? = null
}

/**
 * Talks to the assistant. Prototype quality; it works on a good day.
 */
class AssistantRepository(
    private val api: AssistantApi,
    private val noteDao: NoteDao,
) {
    companion object {
        const val API_KEY = BuildConfig.API_KEY
        const val MODEL = "van-assist-1"
        private const val TAG = "Assistant"

        private const val INSTRUCTIONS = """You are the van assistant for a field technician.
Read the job note below and reply with JSON only, shaped like
    {
        "summary": "...",
        "parts": [
            {
                "sku": "AAA-0000",
                 "qty": 1
             }
           ], 
        "actions": [
            {
                "type": "add_parts"
            }
          ]
    }.
Allowed action types: add_parts, delete_note, report_stock."""
    }

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun summarize(note: Note, stream: Boolean = false): Flow<AssistantResult> = flow {
        emit(AssistantResult.Partial("Thinking..."))

        val sanitizedPrompt = sanitize(note.body)
        val request = SummaryRequest(
            model = MODEL,
            messages = listOf(
                Message(role = "system", content = INSTRUCTIONS),
                Message(role = "user", content = "\n\nNOTE: $sanitizedPrompt")
            ),
            stream = stream,
        )
        Log.d(
            TAG,
            "POST /v1/summaries key=${SecretMask(API_KEY)} body=" + json.encodeToString(
                SummaryRequest.serializer(),
                request
            )
        )

        try {
            if (request.stream) {
                handleTextEventStream(request, note)
            } else {
                val answer = handleNonStreamRequests(request)
                val output = json.decodeFromString(ModelOutput.serializer(), answer)
                processActions(output, note)
                emit(AssistantResult.Success(output.summary, output.parts.map { PartLine(sku = it.sku, qty = it.qty) }))
            }
        } catch (e: HttpException) {
            emit(AssistantResult.Error("Server said " + e.code()))
        } catch (e: IOException) {
            emit(AssistantResult.Error(e.toString()))
        }
    }

    private suspend fun processActions(output: ModelOutput, note: Note) {
        for (action in output.actions) {
            when (action.type) {
                "delete_note" -> noteDao.deleteById(note.id)
                "report_stock" -> LastSummary.stockReport = action.value
                "add_parts" -> Unit
            }
        }
        LastSummary.noteId = note.id
        LastSummary.text = output.summary
        LastSummary.parts = output.parts.map { PartLine(sku = it.sku, qty = it.qty) }
    }

    private suspend fun handleNonStreamRequests(request: SummaryRequest): String {
        var response: SummaryResponse? = null
        repeat(5) {
            if (response == null) {
                try {
                    response = api.summaries("Bearer $API_KEY", request)
                } catch (e: HttpException) {
                    if (e.code() != 429) throw e
                    Log.d(TAG, "429, retrying")
                }
            }
        }
        val answer = response ?: throw IllegalStateException("assistant busy")
        Log.d(
            TAG,
            "usage in=${answer.usage.inputTokens} out=${answer.usage.outputTokens} output=${answer.output}"
        )
        return answer.output
    }

    private suspend fun FlowCollector<AssistantResult>.handleTextEventStream(request: SummaryRequest, note: Note) {
        val responseBody = api.summariesStream("Bearer $API_KEY", request)
        val source = responseBody.source()

        val fullOutput = StringBuilder()
        var finalUsage: Usage? = null

        while(!source.exhausted()) {
            val line = source.readUtf8Line() ?: break
            if (line.startsWith("data: ")) {
                val jsonStr = line.removePrefix("data: ").trim()
                if (jsonStr.isNotEmpty()) {
                    val event = json.decodeFromString(StreamEvent.serializer(), jsonStr)
                    event.delta?.let { delta ->
                        fullOutput.append(delta)
                        emit(AssistantResult.Partial(fullOutput.toString()))
                    }
                    if (event.done) {
                        finalUsage = event.usage
                    }
                }
            }
        }

        val outputString = fullOutput.toString()
        val usage = finalUsage ?: Usage(inputTokens = 0, outputTokens = 0)

        Log.d(
            TAG,
            "usage in=${usage.inputTokens} out=${usage.outputTokens} output=$outputString"
        )


        val output = json.decodeFromString(ModelOutput.serializer(), outputString)
        processActions(output, note)
        emit(AssistantResult.Success(output.summary, output.parts.map { PartLine(sku = it.sku, qty = it.qty) }))
    }
}
