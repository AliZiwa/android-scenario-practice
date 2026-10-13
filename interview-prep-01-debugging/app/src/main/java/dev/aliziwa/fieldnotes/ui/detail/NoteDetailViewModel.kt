package dev.aliziwa.fieldnotes.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aliziwa.fieldnotes.data.NotesRepository
import dev.aliziwa.fieldnotes.domain.Note
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteDetailViewModel(
    private val noteId: Long,
    private val repository: NotesRepository,
) : ViewModel() {

    val note: StateFlow<Note?> = repository
        .observeNote(noteId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun appendBody(text: String) {
        val current = note.value ?: return
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
