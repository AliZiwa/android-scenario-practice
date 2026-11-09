package dev.aliziwa.partsbin

import android.app.Application

class PartsBinApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
