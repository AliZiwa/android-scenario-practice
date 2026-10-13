package dev.aliziwa.fieldnotes

import android.app.Application
import dev.aliziwa.fieldnotes.di.AppContainer

class FieldNotesApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
