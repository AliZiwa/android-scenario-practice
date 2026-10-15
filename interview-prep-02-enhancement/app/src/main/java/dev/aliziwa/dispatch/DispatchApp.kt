package dev.aliziwa.dispatch

import android.app.Application
import dev.aliziwa.dispatch.di.AppContainer

class DispatchApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
