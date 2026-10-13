package dev.aliziwa.fieldnotes

import dev.aliziwa.fieldnotes.data.NotesRepository
import dev.aliziwa.fieldnotes.domain.Note
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory repository for ViewModel tests. [syncGate] lets a test hold a sync open and release it,
 * which is how you reproduce overlapping syncs deterministically.
 */
class FakeNotesRepository : NotesRepository {

    val notes = MutableStateFlow<List<Note>>(emptyList())
    var syncGate: CompletableDeferred<Unit>? = null
    var syncCalls = 0
        private set

    override fun observeNotes(): Flow<List<Note>> = notes

    override fun observeNote(id: Long): Flow<Note> =
        notes.map { list -> list.first { it.id == id } }

    override suspend fun create(title: String, body: String): Long {
        val id = (notes.value.maxOfOrNull { it.id } ?: 0L) + 1
        notes.value = notes.value + Note(id, null, title, body, updatedAt = id)
        return id
    }

    override suspend fun update(note: Note) {
        notes.value = notes.value.map { if (it.id == note.id) note else it }
    }

    override suspend fun delete(id: Long) {
        notes.value = notes.value.filterNot { it.id == id }
    }

    override suspend fun syncFromRemote(): Int {
        syncCalls++
        syncGate?.await()
        return 0
    }
}
