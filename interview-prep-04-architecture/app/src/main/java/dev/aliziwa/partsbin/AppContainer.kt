package dev.aliziwa.partsbin

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.room.Room
import dev.aliziwa.partsbin.data.local.db.PartsDatabase
import dev.aliziwa.partsbin.data.remote.DefaultPartsRepository
import dev.aliziwa.partsbin.data.remote.FakePartsBackend
import dev.aliziwa.partsbin.data.remote.PartsService
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Process-wide state. Whatever needs to be reached from anywhere lives here.
 */
class AppContainer(context: Context) {
    companion object {
        var offline = mutableStateOf(false)
    }
    private val json = Json { ignoreUnknownKeys = true }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://inventory.example/")
            .client(OkHttpClient.Builder().addInterceptor(FakePartsBackend()).build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    private val db: PartsDatabase by lazy {
        Room.databaseBuilder(
            context,
            PartsDatabase::class.java, "parts.db"
        ).build()
    }

    private val partsService: PartsService by lazy { retrofit.create(PartsService::class.java) }

    val partsRepository = DefaultPartsRepository(partsService, db.partDao())
}
