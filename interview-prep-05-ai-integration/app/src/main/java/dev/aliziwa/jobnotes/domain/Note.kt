package dev.aliziwa.jobnotes.domain

/** A technician's job note. `body` is free text typed in the van, often with customer details in it. */
data class Note(
    val id: String,
    val title: String,
    val body: String,
    val updatedAt: String, // ISO-8601
)

/** What the assistant produces for a note. */
data class JobSummary(
    val summary: String,
    val parts: List<PartLine>,
)

data class PartLine(
    val sku: String,
    val qty: Int,
)
