package dev.aliziwa.jobnotes.domain

import kotlinx.coroutines.flow.Flow

interface NotesRepository {
    fun observeNotes(): Flow<List<Note>>
    fun observeNote(id: String): Flow<Note?>
    suspend fun save(note: Note)
    suspend fun delete(id: String)
}
