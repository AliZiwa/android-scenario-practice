package dev.aliziwa.timesheet.data

import android.content.Context

fun interface RateProvider {
    fun getRate(): Double
}

class SharedPreferencesRateProvider(private val context: Context) : RateProvider {
    override fun getRate(): Double {
        return context.getSharedPreferences("timesheet", 0)
            .getFloat("hourly_rate", 42.0f)
            .toDouble()
    }
}
