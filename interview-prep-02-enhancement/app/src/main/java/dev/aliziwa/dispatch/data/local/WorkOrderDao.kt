package dev.aliziwa.dispatch.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Abstract class rather than interface so [replaceAll] can be a real transaction with a body.
 * Tests implement it directly ([dev.aliziwa.dispatch.InMemoryWorkOrderDao] in app/src/test).
 */
@Dao
abstract class WorkOrderDao {

    @Query("SELECT * FROM work_orders ORDER BY updatedAt DESC")
    abstract fun observeAll(): Flow<List<WorkOrderEntity>>

    @Query("SELECT * FROM work_orders WHERE id = :id")
    abstract fun observeById(id: String): Flow<WorkOrderEntity?>

    @Query("SELECT * FROM work_orders")
    abstract suspend fun getAllOnce(): List<WorkOrderEntity>

    @Query("SELECT * FROM work_orders WHERE id = :id")
    abstract suspend fun getByIdOnce(id: String): WorkOrderEntity?

    @Upsert
    abstract suspend fun upsertAll(entities: List<WorkOrderEntity>)

    @Upsert
    abstract suspend fun upsert(entity: WorkOrderEntity)

    @Query("DELETE FROM work_orders")
    abstract suspend fun deleteAll()

    /** The cache mirrors the server: whatever the server returned is the whole truth. */
    @Transaction
    open suspend fun replaceAll(entities: List<WorkOrderEntity>) {
        deleteAll()
        upsertAll(entities)
    }
}
