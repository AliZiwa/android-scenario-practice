package dev.aliziwa.partsbin.domain

/** One row of the list, ready to draw. */
data class PartRow(
    val id: String,
    val sku: String,
    val name: String,
    val quantity: Int,
    val minQuantity: Int = 0,
    val updatedAt: String = "",
    val bin: String,
    val lowStock: Boolean,
    val version: Int = 0,
)
