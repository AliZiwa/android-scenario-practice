package dev.aliziwa.timesheet

import android.content.Context
import androidx.room.Room
import dev.aliziwa.timesheet.data.ShiftRepository
import dev.aliziwa.timesheet.data.local.TimesheetDatabase
import dev.aliziwa.timesheet.data.remote.FakeTimesheetBackend
import dev.aliziwa.timesheet.data.remote.TimesheetApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Service locator initializing singleton infrastructure dependencies.
 */
object Injector {

    lateinit var db: TimesheetDatabase
    lateinit var api: TimesheetApi
    lateinit var repository: ShiftRepository
    var isInitialized = false

    private val json = Json { ignoreUnknownKeys = true }

    fun init(ctx: Context) {
        if (isInitialized) return
        db = Room.databaseBuilder(ctx.applicationContext, TimesheetDatabase::class.java, "timesheet.db").build()
        val client = OkHttpClient.Builder().addInterceptor(FakeTimesheetBackend()).build()
        api = Retrofit.Builder()
            .baseUrl("https://timesheet.example/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TimesheetApi::class.java)
        repository = ShiftRepository(db.shiftDao(), api)
        isInitialized = true
    }
}
