package dev.aliziwa.timesheet

import android.annotation.SuppressLint
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
 * Service locator. Everything reaches in here for what it needs.
 */
@SuppressLint("StaticFieldLeak")
object Injector {

    lateinit var context: Context
    lateinit var db: TimesheetDatabase
    lateinit var api: TimesheetApi
    lateinit var repository: ShiftRepository
    var currentUser: String = "tech-7"
    var isInitialized = false

    fun init(ctx: Context) {
        if (isInitialized) return
        context = ctx
        db = Room.databaseBuilder(ctx, TimesheetDatabase::class.java, "timesheet.db").build()
        val client = OkHttpClient.Builder().addInterceptor(FakeTimesheetBackend()).build()
        api = Retrofit.Builder()
            .baseUrl("https://timesheet.example/")
            .client(client)
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TimesheetApi::class.java)
        repository = ShiftRepository(ctx)
        isInitialized = true
    }
}
