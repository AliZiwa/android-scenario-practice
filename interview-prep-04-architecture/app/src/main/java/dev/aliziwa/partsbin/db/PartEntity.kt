package dev.aliziwa.partsbin.db

import androidx.room.Entity
import androidx.room.PrimaryKey

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
)
