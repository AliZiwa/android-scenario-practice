package dev.aliziwa.partsbin.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aliziwa.partsbin.Globals
import dev.aliziwa.partsbin.db.PartEntity
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
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** One row of the list, ready to draw. */
data class PartRow(
    val id: String,
    val sku: String,
    val name: String,
    val quantity: Int,
    val bin: String,
    val lowStock: Boolean,
)

class PartsListViewModel : ViewModel() {

    private val service: PartsService = Retrofit.Builder()
        .baseUrl("https://inventory.example/")
        .client(OkHttpClient.Builder().addInterceptor(FakePartsBackend()).build())
        .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(PartsService::class.java)

    val rows = MutableStateFlow<List<PartRow>>(emptyList())
    val loading = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)
    val query = MutableStateFlow("")

    private var all: List<PartDto> = emptyList()

    init {
        load()
    }

    fun load() {
        loading.value = true
        viewModelScope.launch {
            try {
                val dtos = withContext(Dispatchers.IO) { service.listParts() }
                all = dtos
                // Keep a copy in Room for later. Nothing reads it yet.
                viewModelScope.launch(Dispatchers.IO) {
                    Globals.db.partDao().upsertAll(
                        dtos.map {
                            PartEntity(it.id, it.sku, it.name, it.quantity, it.minQuantity, it.binLocation, it.version, it.updatedAt)
                        },
                    )
                }
                applyFilter()
                message.value = null
            } catch (e: Exception) {
                all = emptyList()
                rows.value = emptyList()
                message.value = "Could not load parts: " + e.message
            }
            loading.value = false
            Globals.listDirty = false
        }
    }

    fun onQueryChange(q: String) {
        query.value = q
        applyFilter()
    }

    private fun applyFilter() {
        val q = query.value.trim().lowercase()
        rows.value = all
            .filter { q.isEmpty() || it.sku.lowercase().contains(q) || it.name.lowercase().contains(q) }
            .map {
                PartRow(
                    id = it.id,
                    sku = it.sku,
                    name = it.name,
                    quantity = it.quantity,
                    bin = it.binLocation,
                    lowStock = it.quantity < it.minQuantity,
                )
            }
    }

    fun select(id: String) {
        Globals.selectedPartId = id
    }
}
