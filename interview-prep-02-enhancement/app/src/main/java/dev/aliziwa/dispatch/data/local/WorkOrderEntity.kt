package dev.aliziwa.dispatch.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.aliziwa.dispatch.domain.Priority
import dev.aliziwa.dispatch.domain.Status
import dev.aliziwa.dispatch.domain.WorkOrder

@Entity(tableName = "work_orders")
data class WorkOrderEntity(
    @PrimaryKey val id: String,
    val title: String,
    val site: String,
    val priority: String,
    val status: String,
    val version: Int,
    val updatedAt: Long,
)

fun WorkOrderEntity.toDomain(): WorkOrder = WorkOrder(
    id = id,
    title = title,
    site = site,
    priority = Priority.valueOf(priority),
    status = Status.valueOf(status),
    version = version,
    updatedAt = updatedAt,
)

fun WorkOrder.toEntity(): WorkOrderEntity = WorkOrderEntity(
    id = id,
    title = title,
    site = site,
    priority = priority.name,
    status = status.name,
    version = version,
    updatedAt = updatedAt,
)
