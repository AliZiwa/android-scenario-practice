package dev.aliziwa.timesheet

import android.app.Application

class TimesheetApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Injector.init(this)
    }
}
