package dev.aliziwa.fieldnotes.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aliziwa.fieldnotes.data.NotesRepository
import dev.aliziwa.fieldnotes.domain.Note
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotesListUiState(
    val notes: List<Note> = emptyList(),
    val isSyncing: Boolean = false,
    val syncCount: Int = 0,
    val error: String? = null,
)

class NotesListViewModel(
    private val repository: NotesRepository,
) : ViewModel() {

    val uiState: StateFlow<NotesListUiState>
        field = MutableStateFlow(NotesListUiState())

    init {
        observeNotes()
        sync()
    }

    private fun observeNotes() {
        viewModelScope.launch {
            repository.observeNotes().collect { notes ->
                uiState.update { it.copy(notes = notes) }
            }
        }
    }

    fun sync() {
        viewModelScope.launch {
            uiState.update { it.copy(isSyncing = true, error = null) }

            try {
                repository.syncFromRemote()
                uiState.update { it.copy(isSyncing = false, syncCount = it.syncCount + 1) }
            } catch (t: Throwable) {
                uiState.update {
                    it.copy(isSyncing = false, error = t.message ?: "Sync failed")
                }
            }
        }
    }

    fun addQuickNote() {
        viewModelScope.launch {
            val n = uiState.value.notes.size + 1
            repository.create(title = "Quick note $n", body = "Captured on device.")
        }
    }
}
