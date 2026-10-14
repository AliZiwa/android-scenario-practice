package dev.aliziwa.fieldnotes.di

import android.content.Context
import androidx.room.Room
import dev.aliziwa.fieldnotes.data.DefaultNotesRepository
import dev.aliziwa.fieldnotes.data.NotesRepository
import dev.aliziwa.fieldnotes.data.local.NotesDatabase
import dev.aliziwa.fieldnotes.data.remote.FakeRemoteNotesApi
import dev.aliziwa.fieldnotes.data.remote.RemoteNotesApi

/**
 * Hand-rolled dependency container. Deliberately simple so the exercise stays about the app, not the DI framework.
 */
class AppContainer(context: Context) {

    private val database: NotesDatabase = Room.databaseBuilder(
        context,
        NotesDatabase::class.java,
        "fieldnotes.db",
    ).build()

    private val remoteApi: RemoteNotesApi = FakeRemoteNotesApi()

    val repository: NotesRepository = DefaultNotesRepository(
        dao = database.noteDao(),
        api = remoteApi,
    )
}
