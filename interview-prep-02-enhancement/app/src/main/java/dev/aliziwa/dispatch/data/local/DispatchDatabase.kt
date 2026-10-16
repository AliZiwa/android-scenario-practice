package dev.aliziwa.dispatch.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Version 1 shipped to technicians in September. Their phones have cached work orders in this schema.
 * Any schema change from here needs a migration; destructive migration is not an option in this product.
 */
@Database(
    version = DispatchDatabase.LATEST_VERSION,
    entities = [WorkOrderEntity::class],
    exportSchema = true,
)
abstract class DispatchDatabase : RoomDatabase() {
    companion object {
        const val LATEST_VERSION = 3
    }

    abstract fun workOrderDao(): WorkOrderDao
}
