package dev.aliziwa.fieldnotes.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.aliziwa.fieldnotes.domain.Note

@Entity(
    tableName = "notes",
    indices = [Index(value = ["remoteId"], unique = true)]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String?,
    val title: String,
    val body: String,
    val updatedAt: Long,
)

fun NoteEntity.toDomain() = Note(
    id = id,
    remoteId = remoteId,
    title = title,
    body = body,
    updatedAt = updatedAt,
)

fun Note.toEntity() = NoteEntity(
    id = id,
    remoteId = remoteId,
    title = title,
    body = body,
    updatedAt = updatedAt,
)
