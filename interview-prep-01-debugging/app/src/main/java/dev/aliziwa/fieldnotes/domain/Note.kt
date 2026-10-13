package dev.aliziwa.fieldnotes.domain

data class Note(
    val id: Long,
    val remoteId: String?,
    val title: String,
    val body: String,
    val updatedAt: Long,
)
