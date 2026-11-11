package dev.aliziwa.partsbin

import android.app.Application
import android.content.Context
import androidx.room.Room
import dev.aliziwa.partsbin.domain.PartsRepository
import dev.aliziwa.partsbin.data.local.db.PartsDatabase
import dev.aliziwa.partsbin.data.remote.DefaultPartsRepository
import dev.aliziwa.partsbin.data.remote.FakePartsBackend
import dev.aliziwa.partsbin.domain.OfflineState
import dev.aliziwa.partsbin.data.remote.PartsService
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AppDependencyContainer(context: Context) {
    val offlineState = OfflineState()

    private val json = Json { ignoreUnknownKeys = true }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://inventory.example/")
            .client(OkHttpClient.Builder().addInterceptor(FakePartsBackend(offlineState)).build())
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

    val partsRepository: PartsRepository by lazy {
        DefaultPartsRepository(partsService, db.partDao())
    }
}

class PartsBinApp : Application() {
    lateinit var container: AppDependencyContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppDependencyContainer(this)
    }
}
