package dev.aliziwa.partsbin.domain

data class Part(
    val id: String,
    val sku: String,
    val name: String,
    val quantity: Int,
    val minQuantity: Int,
    val updatedAt: String,
    val bin: String,
    val version: Int = 0,
) {
    val lowStock: Boolean
        get() = quantity <= minQuantity
}
