package dev.aliziwa.partsbin.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PartDao {

    @Query("SELECT * FROM parts ORDER BY sku")
    fun observeAll(): Flow<List<PartEntity>>

    @Query("SELECT * FROM parts ORDER BY sku")
    suspend fun getAll(): List<PartEntity>

    @Query("SELECT * FROM parts WHERE id = :id")
    fun observeById(id: String): Flow<PartEntity?>

    @Query("SELECT * FROM parts WHERE id = :id")
    suspend fun getById(id: String): PartEntity?

    @Upsert
    suspend fun upsertAll(parts: List<PartEntity>)

    @Upsert
    suspend fun upsert(part: PartEntity)

    @Query("DELETE FROM parts")
    suspend fun deleteAll()
}
