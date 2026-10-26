package dev.aliziwa.timesheet.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec

@Database(
    entities = [ShiftEntity::class],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = TimesheetDatabase.DeleteTmpSpec::class),
    ],
    exportSchema = true,
)
abstract class TimesheetDatabase : RoomDatabase() {

    @DeleteColumn(tableName = "shifts", columnName = "tmp")
    class DeleteTmpSpec : AutoMigrationSpec

    abstract fun shiftDao(): ShiftDao
}
