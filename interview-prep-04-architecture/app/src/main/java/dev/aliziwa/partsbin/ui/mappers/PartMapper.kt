package dev.aliziwa.partsbin.ui.mappers

import dev.aliziwa.partsbin.domain.Part
import dev.aliziwa.partsbin.ui.models.PartRow

fun Part.toRow(): PartRow {
    val instant = java.time.Instant.parse(updatedAt)
    val localDateTime = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
    val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")

    return PartRow(
        id = id,
        sku = sku,
        name = name,
        quantity = quantity,
        minQuantity = minQuantity,
        bin = bin,
        updatedAt = localDateTime.format(formatter),
        version = version,
        lowStock = lowStock,
    )
}
