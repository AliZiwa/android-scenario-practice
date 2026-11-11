package dev.aliziwa.jobnotes.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.aliziwa.jobnotes.domain.Note

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val updatedAt: String,
) {
    fun toDomain() = Note(id = id, title = title, body = body, updatedAt = updatedAt)

    companion object {
        fun fromDomain(note: Note) = NoteEntity(
            id = note.id,
            title = note.title,
            body = note.body,
            updatedAt = note.updatedAt,
        )
    }
}
