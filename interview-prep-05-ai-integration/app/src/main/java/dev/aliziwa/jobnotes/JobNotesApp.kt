package dev.aliziwa.jobnotes

import android.app.Application
import android.content.Context
import androidx.room.Room
import dev.aliziwa.jobnotes.data.assistant.AssistantApi
import dev.aliziwa.jobnotes.data.assistant.AssistantRepository
import dev.aliziwa.jobnotes.data.assistant.FakeAssistantBackend
import dev.aliziwa.jobnotes.data.local.JobNotesDatabase
import dev.aliziwa.jobnotes.data.local.RoomNotesRepository
import dev.aliziwa.jobnotes.data.local.Seed
import dev.aliziwa.jobnotes.domain.NotesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** Composition root. One of everything, built lazily. */
class AppContainer(context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    val assistantBackend = FakeAssistantBackend()

    private val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(assistantBackend)
            // The assistant can be slow. Never give up on it.
            .readTimeout(0, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://assistant.example/")
            .client(okHttp)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    private val db: JobNotesDatabase by lazy {
        Room.databaseBuilder(context, JobNotesDatabase::class.java, "jobnotes.db").build()
    }

    val notesRepository: NotesRepository by lazy { RoomNotesRepository(db.noteDao()) }

    val assistantRepository: AssistantRepository by lazy {
        AssistantRepository(retrofit.create(AssistantApi::class.java), db.noteDao())
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** First launch: put the seed notes in. */
    fun seedIfEmpty() {
        appScope.launch {
            val dao = db.noteDao()
            if (dao.count() == 0) dao.upsertAll(Seed.NOTES)
        }
    }
}

class JobNotesApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.seedIfEmpty()
    }
}
