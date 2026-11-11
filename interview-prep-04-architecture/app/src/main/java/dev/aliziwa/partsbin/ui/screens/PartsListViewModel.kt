package dev.aliziwa.partsbin.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.aliziwa.partsbin.PartsBinApp
import dev.aliziwa.partsbin.domain.PartsRepository
import dev.aliziwa.partsbin.domain.RefreshResult
import dev.aliziwa.partsbin.ui.mappers.toRow
import dev.aliziwa.partsbin.ui.models.PartRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PartsListState {
    data object Loading : PartsListState
    data class Loaded(val rows: List<PartRow>, val message: String? = null) : PartsListState
}

class PartsListViewModel(
    private val repository: PartsRepository,
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

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PartsListState> = combine(
        repository.observeParts(),
        query,
        errorMessage,
    ) { parts, q, msg ->
        val trimmedQuery = q.trim().lowercase()
        val filteredParts = if (trimmedQuery.isEmpty()) {
            parts
        } else {
            parts.filter {
                it.sku.lowercase().contains(trimmedQuery) || it.name.lowercase().contains(trimmedQuery)
            }
        }
        PartsListState.Loaded(rows = filteredParts.map { it.toRow() }, message = msg)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PartsListState.Loading,
    )

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val result = repository.refreshParts()
            when (result) {
                is RefreshResult.Success -> errorMessage.value = null
                is RefreshResult.Offline -> errorMessage.value = "Offline, showing cached parts"
                is RefreshResult.Error -> errorMessage.value = "Could not refresh"
            }
        }
    }

    fun onQueryChange(q: String) {
        _query.value = q
    }
}
