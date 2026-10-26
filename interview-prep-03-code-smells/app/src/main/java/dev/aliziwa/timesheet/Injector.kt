package dev.aliziwa.timesheet

import android.content.Context
import androidx.room.Room
import dev.aliziwa.timesheet.data.RateProvider
import dev.aliziwa.timesheet.data.SharedPreferencesRateProvider
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
    lateinit var rateProvider: RateProvider
    var isInitialized = false

    private val json = Json { ignoreUnknownKeys = true }

    fun init(ctx: Context) {
        if (isInitialized) return
        val appContext = ctx.applicationContext
        db = Room.databaseBuilder(appContext, TimesheetDatabase::class.java, "timesheet.db").build()
        val client = OkHttpClient.Builder().addInterceptor(FakeTimesheetBackend()).build()
        api = Retrofit.Builder()
            .baseUrl("https://timesheet.example/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TimesheetApi::class.java)
        repository = ShiftRepository(db.shiftDao(), api)
        rateProvider = SharedPreferencesRateProvider(appContext)
        isInitialized = true
    }
}
