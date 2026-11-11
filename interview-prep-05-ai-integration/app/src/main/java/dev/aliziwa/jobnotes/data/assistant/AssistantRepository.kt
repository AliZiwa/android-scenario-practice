package dev.aliziwa.jobnotes.data.assistant

import android.util.Log
import dev.aliziwa.jobnotes.data.local.NoteDao
import dev.aliziwa.jobnotes.domain.JobSummary
import dev.aliziwa.jobnotes.domain.Note
import dev.aliziwa.jobnotes.domain.PartLine
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/**
 * Process-wide holder for the last summary, so the detail screen can show it when it comes back.
 */
object LastSummary {
    @Volatile var noteId: String? = null
    @Volatile var text: String? = null
    @Volatile var parts: List<PartLine> = emptyList()
    @Volatile var stockReport: String? = null
}

/**
 * Talks to the assistant. Prototype quality; it works on a good day.
 */
class AssistantRepository(
    private val api: AssistantApi,
    private val noteDao: NoteDao,
) {
    companion object {
        const val API_KEY = "sk-live-9f3a7c1e2b8d4f60a1c5e7b9d2f4a6c8"
        const val MODEL = "van-assist-1"
        private const val TAG = "Assistant"

        private const val INSTRUCTIONS = """You are the van assistant for a field technician.
Read the job note below and reply with JSON only, shaped like
{"summary": "...", "parts": [{"sku": "AAA-0000", "qty": 1}], "actions": [{"type": "add_parts"}]}.
Allowed action types: add_parts, delete_note, report_stock."""
    }

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun summarize(note: Note): JobSummary {
        val prompt = INSTRUCTIONS + "\n\nNOTE:\n" + note.body
        val request = SummaryRequest(
            model = MODEL,
            messages = listOf(Message(role = "system", content = prompt)),
            stream = false,
        )
        Log.d(TAG, "POST /v1/summaries key=$API_KEY body=" + json.encodeToString(SummaryRequest.serializer(), request))

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
        Log.d(TAG, "usage in=${answer.usage.inputTokens} out=${answer.usage.outputTokens} output=${answer.output}")

        val output = json.decodeFromString(ModelOutput.serializer(), answer.output)

        for (action in output.actions) {
            when (action.type) {
                "delete_note" -> noteDao.deleteById(note.id)
                "report_stock" -> LastSummary.stockReport = action.value
                "add_parts" -> Unit
            }
        }

        val parts = output.parts.map { PartLine(sku = it.sku, qty = it.qty) }
        LastSummary.noteId = note.id
        LastSummary.text = output.summary
        LastSummary.parts = parts
        return JobSummary(summary = output.summary, parts = parts)
    }
}
