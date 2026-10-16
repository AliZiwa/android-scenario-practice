package dev.aliziwa.timesheet.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {

    @Query("SELECT * FROM shifts ORDER BY date DESC, startMinutes DESC")
    fun observeAll(): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts ORDER BY date DESC, startMinutes DESC")
    suspend fun getAll(): List<ShiftEntity>

    @Query("SELECT * FROM shifts WHERE id = :id")
    suspend fun getById(id: Long): ShiftEntity?

    @Query("SELECT * FROM shifts WHERE status = :status")
    suspend fun getByStatus(status: String): List<ShiftEntity>

    @Insert
    suspend fun insert(shift: ShiftEntity): Long

    @Update
    suspend fun update(shift: ShiftEntity)

    @Query("DELETE FROM shifts WHERE id = :id")
    suspend fun deleteById(id: Long)
}
