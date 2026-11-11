package dev.aliziwa.partsbin.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.aliziwa.partsbin.domain.PartRow

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
    fun toRow(): PartRow {
        val instant = java.time.Instant.parse(updatedAt)
        val localDateTime = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")
        
        return PartRow(
            id = id,
            sku = sku,
            name = name,
            quantity = quantity,
            minQuantity = minQuantity,
            bin = binLocation,
            updatedAt = localDateTime.format(formatter),
            version = version,
            lowStock = quantity <= minQuantity,
        )
    }
}
