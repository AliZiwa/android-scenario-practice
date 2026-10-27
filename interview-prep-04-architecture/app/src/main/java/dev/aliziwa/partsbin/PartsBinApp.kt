package dev.aliziwa.partsbin

import android.app.Application

class PartsBinApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Globals.app = this
    }
}
