package dev.aliziwa.fieldnotes.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aliziwa.fieldnotes.data.NotesRepository
import dev.aliziwa.fieldnotes.domain.Note
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface NoteDetailUiState {
    object Loading : NoteDetailUiState
    data class Loaded(val note: Note) : NoteDetailUiState
    object Gone : NoteDetailUiState
}

class NoteDetailViewModel(
    private val noteId: Long,
    private val repository: NotesRepository,
) : ViewModel() {

    val noteUiState: StateFlow<NoteDetailUiState> = repository
        .observeNote(noteId)
        .map {
            when (it) {
                null -> NoteDetailUiState.Gone
                else -> NoteDetailUiState.Loaded(it)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            NoteDetailUiState.Loading
        )

    fun appendBody(text: String) {
        val current = (noteUiState.value as? NoteDetailUiState.Loaded)?.note ?: return
        viewModelScope.launch {
            repository.update(
                current.copy(
                    body = current.body + "\n" + text,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun delete() {
        viewModelScope.launch {
            repository.delete(noteId)
        }
    }
}
