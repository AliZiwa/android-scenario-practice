package dev.aliziwa.jobnotes.data.local

import dev.aliziwa.jobnotes.domain.Note
import dev.aliziwa.jobnotes.domain.NotesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomNotesRepository(private val dao: NoteDao) : NotesRepository {

    override fun observeNotes(): Flow<List<Note>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeNote(id: String): Flow<Note?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun save(note: Note) = dao.upsert(NoteEntity.fromDomain(note))

    override suspend fun delete(id: String) = dao.deleteById(id)
}
