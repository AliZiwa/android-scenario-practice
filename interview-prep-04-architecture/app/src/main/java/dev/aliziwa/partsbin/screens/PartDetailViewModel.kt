package dev.aliziwa.partsbin.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aliziwa.partsbin.Globals
import dev.aliziwa.partsbin.net.AdjustRequest
import dev.aliziwa.partsbin.net.FakePartsBackend
import dev.aliziwa.partsbin.net.PartDto
import dev.aliziwa.partsbin.net.PartsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class PartDetailViewModel : ViewModel() {

    private val service: PartsService = Retrofit.Builder()
        .baseUrl("https://inventory.example/")
        .client(OkHttpClient.Builder().addInterceptor(FakePartsBackend()).build())
        .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(PartsService::class.java)

    val part = MutableStateFlow<PartDto?>(null)
    val loading = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)

    fun load() {
        val id = Globals.selectedPartId ?: run {
            message.value = "No part selected"
            return
        }
        loading.value = true
        viewModelScope.launch {
            try {
                part.value = withContext(Dispatchers.IO) { service.getPart(id) }
            } catch (e: Exception) {
                message.value = "Could not load part: " + e.message
            }
            loading.value = false
        }
    }

    fun isLowStock(): Boolean {
        val p = part.value ?: return false
        return p.quantity <= p.minQuantity
    }

    fun adjust(delta: Int, reason: String) {
        val id = Globals.selectedPartId ?: return
        busy.value = true
        viewModelScope.launch {
            try {
                val updated = withContext(Dispatchers.IO) { service.adjust(id, AdjustRequest(delta, reason)) }
                part.value = updated
                message.value = "Stock now " + updated.quantity
                Globals.listDirty = true
            } catch (e: HttpException) {
                message.value = "Server said " + e.code()
            } catch (e: Exception) {
                message.value = e.toString()
            }
            busy.value = false
        }
    }

    fun clearMessage() {
        message.value = null
    }
}
