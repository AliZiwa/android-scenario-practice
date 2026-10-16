package dev.aliziwa.timesheet

import dev.aliziwa.timesheet.data.local.ShiftDao
import dev.aliziwa.timesheet.data.local.ShiftEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * JVM stand-in for the Room DAO. Same ordering as the real queries (date desc, start desc).
 * Stores copies, like Room does, so later mutation of an entity you inserted is not visible here
 * until you call [update].
 */
class InMemoryShiftDao : ShiftDao {

    val rows = MutableStateFlow<Map<Long, ShiftEntity>>(emptyMap())
    private var nextId = 1L

    private fun ordered(map: Map<Long, ShiftEntity>): List<ShiftEntity> =
        map.values.sortedWith(compareByDescending<ShiftEntity> { it.date }.thenByDescending { it.startMinutes })

    override fun observeAll(): Flow<List<ShiftEntity>> = rows.map { ordered(it) }

    override suspend fun getAll(): List<ShiftEntity> = ordered(rows.value)

    override suspend fun getById(id: Long): ShiftEntity? = rows.value[id]?.copy()

    override suspend fun getByStatus(status: String): List<ShiftEntity> =
        ordered(rows.value).filter { it.status == status }.map { it.copy() }

    override suspend fun insert(shift: ShiftEntity): Long {
        val id = if (shift.id == 0L) nextId++ else shift.id
        rows.update { it + (id to shift.copy(id = id)) }
        return id
    }

    override suspend fun update(shift: ShiftEntity) {
        rows.update { current ->
            if (current.containsKey(shift.id)) current + (shift.id to shift.copy()) else current
        }
    }

    override suspend fun deleteById(id: Long) {
        rows.update { it - id }
    }
}
