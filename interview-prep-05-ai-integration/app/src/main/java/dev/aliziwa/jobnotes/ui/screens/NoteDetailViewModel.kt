package dev.aliziwa.jobnotes.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import dev.aliziwa.jobnotes.JobNotesApp
import dev.aliziwa.jobnotes.data.assistant.AssistantRepository
import dev.aliziwa.jobnotes.data.assistant.LastSummary
import dev.aliziwa.jobnotes.domain.Note
import dev.aliziwa.jobnotes.domain.NotesRepository
import dev.aliziwa.jobnotes.domain.PartLine
import dev.aliziwa.jobnotes.ui.NoteDetail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.time.Instant

sealed interface SummaryUiState {
    data object Idle : SummaryUiState
    data object Working : SummaryUiState
    data class Done(val summary: String, val parts: List<PartLine>) : SummaryUiState
    data class Failed(val message: String) : SummaryUiState
}

class NoteDetailViewModel(
    private val notes: NotesRepository,
    private val assistant: AssistantRepository,
    private val noteId: String,
) : ViewModel() {

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as JobNotesApp
                val id = createSavedStateHandle().toRoute<NoteDetail>().noteId
                NoteDetailViewModel(app.container.notesRepository, app.container.assistantRepository, id)
            }
        }
    }

    // The assistant call must outlive the screen, otherwise the answer is wasted.
    private val workScope = CoroutineScope(Dispatchers.IO)

    val note: StateFlow<Note?> = notes.observeNote(noteId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _summary = MutableStateFlow<SummaryUiState>(SummaryUiState.Idle)
    val summary: StateFlow<SummaryUiState> = _summary.asStateFlow()

    init {
        // Show whatever the assistant said last, so the screen is not empty while it thinks.
        val last = LastSummary.text
        if (last != null) {
            _summary.value = SummaryUiState.Done(last, LastSummary.parts)
        }
    }

    fun summarize() {
        val current = note.value ?: return
        _summary.value = SummaryUiState.Working
        workScope.launch {
            try {
                val result = assistant.summarize(current)
                _summary.value = SummaryUiState.Done(result.summary, result.parts)
            } catch (e: HttpException) {
                _summary.value = SummaryUiState.Failed("Server said " + e.code())
            } catch (e: IOException) {
                _summary.value = SummaryUiState.Failed(e.toString())
            }
        }
    }

    fun saveBody(body: String) {
        val current = note.value ?: return
        viewModelScope.launch {
            notes.save(current.copy(body = body, updatedAt = Instant.now().toString()))
        }
    }
}
