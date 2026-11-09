package dev.aliziwa.partsbin.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.aliziwa.partsbin.PartsBinApp
import dev.aliziwa.partsbin.data.PartsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** One row of the list, ready to draw. */
data class PartRow(
    val id: String,
    val sku: String,
    val name: String,
    val quantity: Int,
    val minQuantity: Int = 0,
    val updatedAt: String = "",
    val bin: String,
    val lowStock: Boolean,
    val version: Int = 0,
)

sealed interface PartsListState {
    data object Loading : PartsListState
    data class Loaded(val rows: List<PartRow>) : PartsListState
    data class Error(val message: String) : PartsListState
}

class PartsListViewModel(
    private val repository: PartsRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as PartsBinApp
                val repository = app.container.partsRepository
                PartsListViewModel(repository)
            }
        }
    }

    val uiState: StateFlow<PartsListState>
        field = MutableStateFlow<PartsListState>(PartsListState.Loading)

    val query = MutableStateFlow("")

    init {
        load()
    }

    fun load() {
        uiState.value = PartsListState.Loading

        viewModelScope.launch(dispatcher) {
            try {
                uiState.value = PartsListState.Loaded(repository.listParts())
                applyFilter()
            } catch (e: Exception) {
                uiState.value = PartsListState.Error("Could not load parts: " + e.message)
            }
        }
    }

    fun onQueryChange(q: String) {
        query.value = q
        applyFilter()
    }

    private fun applyFilter() {
        val q = query.value.trim().lowercase()
        if (q.isNotEmpty() && uiState.value is PartsListState.Loaded) {
            (uiState.value as PartsListState.Loaded).rows
                .filter { q.isEmpty() || it.sku.lowercase().contains(q) || it.name.lowercase().contains(q) }
        }
    }
}
