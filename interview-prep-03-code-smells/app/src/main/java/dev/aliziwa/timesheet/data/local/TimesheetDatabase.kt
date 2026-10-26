package dev.aliziwa.timesheet.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ShiftEntity::class], version = 1, exportSchema = true)
abstract class TimesheetDatabase : RoomDatabase() {
    abstract fun shiftDao(): ShiftDao
}
