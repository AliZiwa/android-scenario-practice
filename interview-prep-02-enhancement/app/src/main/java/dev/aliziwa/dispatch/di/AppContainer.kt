package dev.aliziwa.dispatch.di

import android.content.Context
import androidx.room.Room
import dev.aliziwa.dispatch.data.DefaultWorkOrdersRepository
import dev.aliziwa.dispatch.data.WorkOrdersRepository
import dev.aliziwa.dispatch.data.local.DispatchDatabase
import dev.aliziwa.dispatch.data.remote.FakeDispatchBackend
import dev.aliziwa.dispatch.data.remote.WorkOrdersApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Hand-rolled dependency container, the same shape as assignment 01. The network stack is real
 * (OkHttp, Retrofit, kotlinx.serialization); only the server is faked, by an interceptor.
 */
class AppContainer(context: Context) {

    val backend = FakeDispatchBackend()

    private val json = Json { ignoreUnknownKeys = true }

    private val okHttp: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(backend)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttp)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val api: WorkOrdersApi = retrofit.create(WorkOrdersApi::class.java)

    private val database: DispatchDatabase = Room.databaseBuilder(
        context.applicationContext,
        DispatchDatabase::class.java,
        "dispatch.db",
    ).build()

    val repository: WorkOrdersRepository = DefaultWorkOrdersRepository(
        dao = database.workOrderDao(),
        api = api,
    )

    private companion object {
        /** Never resolved: [FakeDispatchBackend] answers before OkHttp opens a socket. */
        const val BASE_URL = "https://dispatch.example/"
    }
}
