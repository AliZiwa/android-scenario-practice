package dev.aliziwa.fieldnotes.data

import dev.aliziwa.fieldnotes.data.local.NoteDao
import dev.aliziwa.fieldnotes.data.local.NoteEntity
import dev.aliziwa.fieldnotes.data.local.toDomain
import dev.aliziwa.fieldnotes.data.local.toEntity
import dev.aliziwa.fieldnotes.data.remote.RemoteNotesApi
import dev.aliziwa.fieldnotes.domain.Note
import dev.aliziwa.fieldnotes.sync.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface NotesRepository {
    fun observeNotes(): Flow<List<Note>>
    fun observeNote(id: Long): Flow<Note>
    suspend fun create(title: String, body: String): Long
    suspend fun update(note: Note)
    suspend fun delete(id: Long)

    /** Pulls notes from the server and stores the ones we don't have yet. Returns how many were added. */
    suspend fun syncFromRemote(): Int
}

class DefaultNotesRepository(
    private val dao: NoteDao,
    private val api: RemoteNotesApi,
) : NotesRepository {

    override fun observeNotes(): Flow<List<Note>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeNote(id: Long): Flow<Note> =
        dao.observeById(id).map { it!!.toDomain() }

    override suspend fun create(title: String, body: String): Long =
        dao.insert(
            NoteEntity(
                remoteId = null,
                title = title,
                body = body,
                updatedAt = System.currentTimeMillis(),
            ),
        )

    override suspend fun update(note: Note) = dao.update(note.toEntity())

    override suspend fun delete(id: Long) = dao.deleteById(id)

    override suspend fun syncFromRemote(): Int = withContext(Dispatchers.IO) {
        val remote = api.fetchNotes()
        val known = dao.getAllOnce().mapNotNull { it.remoteId }.toSet()
        var added = 0
        for (r in remote) {
            if (r.id !in known) {
                dao.insert(
                    NoteEntity(
                        remoteId = r.id,
                        title = r.title,
                        body = r.body,
                        updatedAt = r.updatedAt,
                    ),
                )
                added++
            }
        }
        withContext(Dispatchers.Main) { SyncManager.notifyFinished(added) }
        added
    }
}
