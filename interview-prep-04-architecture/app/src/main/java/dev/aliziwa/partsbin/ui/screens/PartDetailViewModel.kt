package dev.aliziwa.partsbin.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.aliziwa.partsbin.PartsBinApp
import dev.aliziwa.partsbin.domain.PartRow
import dev.aliziwa.partsbin.domain.PartsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface PartsDetailState {
    data object Loading : PartsDetailState
    data class Loaded(val rowItem: PartRow, val message: String? = null) : PartsDetailState
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
                    val currentMessage = (uiState.value as? PartsDetailState.Loaded)?.message
                    uiState.value = PartsDetailState.Loaded(part, currentMessage)
                }
            }
        }
    }

    fun adjust(selectedPartId: String, delta: Int, reason: String) {
        busy.value = true
        viewModelScope.launch(dispatcher) {
            try {
                repository.adjust(selectedPartId, delta, reason)
            } catch (e: IllegalArgumentException) {
                if (uiState.value is PartsDetailState.Loaded) {
                     val currentPart = (uiState.value as PartsDetailState.Loaded).rowItem
                     uiState.value = PartsDetailState.Loaded(currentPart, e.message ?: "Invalid adjustment")
                } else {
                     uiState.value = PartsDetailState.Error(e.message ?: "Invalid adjustment")
                }
            } catch (e: dev.aliziwa.partsbin.domain.ServerException) {
                if (uiState.value is PartsDetailState.Loaded) {
                     val currentPart = (uiState.value as PartsDetailState.Loaded).rowItem
                     uiState.value = PartsDetailState.Loaded(currentPart, e.message ?: "Server Error")
                } else {
                     uiState.value = PartsDetailState.Error(e.message ?: "Server Error")
                }
            } catch (e: java.io.IOException) {
                if (uiState.value is PartsDetailState.Loaded) {
                     val currentPart = (uiState.value as PartsDetailState.Loaded).rowItem
                     uiState.value = PartsDetailState.Loaded(currentPart, "Offline, adjustment not saved")
                } else {
                     uiState.value = PartsDetailState.Error("Offline, adjustment not saved")
                }
            } finally {
                busy.value = false
            }
        }
    }
    
    fun dismissMessage() {
        if (uiState.value is PartsDetailState.Loaded) {
            val currentPart = (uiState.value as PartsDetailState.Loaded).rowItem
            uiState.value = PartsDetailState.Loaded(currentPart, null)
        }
    }
}
