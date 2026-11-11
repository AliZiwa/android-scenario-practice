package dev.aliziwa.partsbin.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import dev.aliziwa.partsbin.PartsBinApp
import dev.aliziwa.partsbin.domain.AdjustmentError
import dev.aliziwa.partsbin.domain.AdjustmentResult
import dev.aliziwa.partsbin.domain.PartsRepository
import dev.aliziwa.partsbin.ui.PartsDetail
import dev.aliziwa.partsbin.ui.mappers.toRow
import dev.aliziwa.partsbin.ui.models.PartRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PartsDetailState {
    data object Loading : PartsDetailState
    data class Loaded(val rowItem: PartRow) : PartsDetailState
}

class PartDetailViewModel(
    private val repository: PartsRepository,
    private val selectedPartId: String
) : ViewModel() {

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as PartsBinApp
                val repository = app.container.partsRepository
                val savedStateHandle = createSavedStateHandle()
                val id = savedStateHandle.toRoute<PartsDetail>().selectedPartId
                PartDetailViewModel(repository, id)
            }
        }
    }

    val uiState: StateFlow<PartsDetailState> = repository.observePart(selectedPartId)
        .map { part -> 
            if (part != null) PartsDetailState.Loaded(part.toRow()) 
            else PartsDetailState.Loading 
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PartsDetailState.Loading
        )

    val message: StateFlow<String?> 
        field = MutableStateFlow<String?>(null)

    val busy: StateFlow<Boolean>
        field = MutableStateFlow(false)

    fun dismissMessage() {
        message.value = null
    }

    fun adjust(delta: Int, reason: String) {
        busy.value = true
        viewModelScope.launch {
            val result = repository.adjust(selectedPartId, delta, reason)
            when (result) {
                is AdjustmentResult.Success -> {
                    message.value = "Stock now ${result.part.quantity}"
                }
                is AdjustmentResult.Rejected -> {
                    message.value = when (result.reason) {
                        AdjustmentError.REASON_REQUIRED -> "Reason required"
                        AdjustmentError.INSUFFICIENT_STOCK -> "Not enough stock"
                    }
                }
                is AdjustmentResult.ServerRejected -> {
                    message.value = result.message
                }
                is AdjustmentResult.NotFound -> {
                    message.value = "Part not found locally"
                }
                is AdjustmentResult.Offline -> {
                    message.value = "Offline, adjustment not saved"
                }
            }
            busy.value = false
        }
    }
}
