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

    private val _uiState = MutableStateFlow(NotesListUiState())
    val uiState: StateFlow<NotesListUiState> = _uiState.asStateFlow()

    init {
        observeNotes()
        sync()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun observeNotes() {
        GlobalScope.launch(Dispatchers.Main) {
            repository.observeNotes().collect { notes ->
                _uiState.update { it.copy(notes = notes) }
            }
        }
    }

    fun sync() {
        viewModelScope.launch {
            val countBefore = _uiState.value.syncCount
            _uiState.update { it.copy(isSyncing = true, error = null) }
            try {
                repository.syncFromRemote()
                _uiState.update { it.copy(isSyncing = false, syncCount = countBefore + 1) }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isSyncing = false, error = t.message ?: "Sync failed") }
            }
        }
    }

    fun addQuickNote() {
        viewModelScope.launch {
            val n = _uiState.value.notes.size + 1
            repository.create(title = "Quick note $n", body = "Captured on device.")
        }
    }
}
