package dev.aliziwa.dispatch.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Version 1 shipped to technicians in September. Their phones have cached work orders in this schema.
 * Any schema change from here needs a migration; destructive migration is not an option in this product.
 */
@Database(
    entities = [WorkOrderEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class DispatchDatabase : RoomDatabase() {
    abstract fun workOrderDao(): WorkOrderDao
}
