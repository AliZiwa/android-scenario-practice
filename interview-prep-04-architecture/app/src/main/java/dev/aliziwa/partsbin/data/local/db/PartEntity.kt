package dev.aliziwa.partsbin.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.aliziwa.partsbin.domain.Part

@Entity(tableName = "parts")
data class PartEntity(
    @PrimaryKey val id: String,
    val sku: String,
    val name: String,
    val quantity: Int,
    val minQuantity: Int,
    val binLocation: String,
    val version: Int,
    val updatedAt: String,
) {
    fun toDomain() = Part(
        id = id,
        sku = sku,
        name = name,
        quantity = quantity,
        minQuantity = minQuantity,
        bin = binLocation,
        updatedAt = updatedAt,
        version = version,
    )
}
