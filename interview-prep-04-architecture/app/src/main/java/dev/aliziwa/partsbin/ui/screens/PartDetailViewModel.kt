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
import retrofit2.HttpException

sealed interface PartsDetailState {
    data object Loading : PartsDetailState
    data class Loaded(val rowItem: PartRow) : PartsDetailState
    data class Error(val message: String) : PartsDetailState
}

class PartDetailViewModel(
    private val repository: PartsRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as PartsBinApp
                val repository = app.container.partsRepository
                PartDetailViewModel(repository)
            }
        }
    }

    val uiState: StateFlow<PartsDetailState>
        field = MutableStateFlow<PartsDetailState>(PartsDetailState.Loading)

    val busy: StateFlow<Boolean>
        field = MutableStateFlow(false)

    fun load(selectedPartId: String) {
        viewModelScope.launch(dispatcher) {
            repository.observePart(selectedPartId).collect { part ->
                if (part != null) {
                    uiState.value = PartsDetailState.Loaded(part)
                }
            }
        }
    }

    fun isLowStock(): Boolean {
        return (uiState.value as? PartsDetailState.Loaded)?.rowItem?.lowStock ?: false
    }

    fun adjust(selectedPartId: String, delta: Int, reason: String) {
        busy.value = true
        viewModelScope.launch(dispatcher) {
            try {
                val updated = repository.adjust(selectedPartId, delta, reason)
                uiState.value = PartsDetailState.Loaded(updated)
            } catch (e: HttpException) {
                uiState.value = PartsDetailState.Error("Server said " + e.code())
            } catch (e: Exception) {
                uiState.value = PartsDetailState.Error(e.toString())
            }
        }
        busy.value = false
    }
}
