package dev.aliziwa.partsbin.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PartEntity::class], version = 1, exportSchema = true)
abstract class PartsDatabase : RoomDatabase() {
    abstract fun partDao(): PartDao
}
