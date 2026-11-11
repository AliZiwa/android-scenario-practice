package dev.aliziwa.jobnotes

import dev.aliziwa.jobnotes.data.local.NoteDao
import dev.aliziwa.jobnotes.data.local.NoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * JVM stand-in for the Room DAO. Same ordering as the real query (updatedAt descending). Flows emit on
 * every write, like Room's do.
 */
class InMemoryNoteDao : NoteDao {

    val rows = MutableStateFlow<Map<String, NoteEntity>>(emptyMap())

    private fun ordered(map: Map<String, NoteEntity>): List<NoteEntity> =
        map.values.sortedByDescending { it.updatedAt }

    override fun observeAll(): Flow<List<NoteEntity>> = rows.map { ordered(it) }

    override fun observeById(id: String): Flow<NoteEntity?> = rows.map { it[id] }

    override suspend fun getById(id: String): NoteEntity? = rows.value[id]

    override suspend fun count(): Int = rows.value.size

    override suspend fun upsert(note: NoteEntity) {
        rows.update { it + (note.id to note) }
    }

    override suspend fun upsertAll(notes: List<NoteEntity>) {
        rows.update { current -> current + notes.associateBy { it.id } }
    }

    override suspend fun deleteById(id: String) {
        rows.update { it - id }
    }
}
