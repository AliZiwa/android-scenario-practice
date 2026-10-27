package dev.aliziwa.timesheet.data

import dev.aliziwa.timesheet.data.local.ShiftDao
import dev.aliziwa.timesheet.data.local.ShiftEntity
import dev.aliziwa.timesheet.data.remote.ShiftDto
import dev.aliziwa.timesheet.data.remote.TimesheetApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface SubmitResult {
    data class Success(val serverId: String, val status: String) : SubmitResult
    data class Failure(val reason: String) : SubmitResult
}

class ShiftRepository(
    private val dao: ShiftDao,
    private val api: TimesheetApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    suspend fun getShifts(): List<ShiftEntity> {
        return withContext(ioDispatcher) {
            dao.getAll()
        }
    }

    suspend fun save(shift: ShiftEntity): Long {
        return withContext(ioDispatcher) {
            if (shift.id == 0L) {
                dao.insert(shift)
            } else {
                dao.update(shift)
                shift.id
            }
        }
    }

    suspend fun delete(id: Long) {
        withContext(ioDispatcher) {
            dao.deleteById(id)
        }
    }

    /**
     * Submits a shift. On success the shift is updated in place and saved in local database.
     */
    suspend fun submit(shift: ShiftEntity): SubmitResult {
        return withContext(ioDispatcher) {
            try {
                val dto = ShiftDto(
                    shift.workOrderId,
                    shift.date,
                    shift.startMinutes,
                    shift.endMinutes,
                    shift.breakMinutes,
                    shift.billable,
                    shift.note,
                )
                val resp = api.submit(dto)
                shift.serverId = resp.serverId
                shift.status = resp.status
                dao.update(shift)
                SubmitResult.Success(resp.serverId, resp.status)
            } catch (e: Exception) {
                SubmitResult.Failure(e.message ?: "Unknown error")
            }
        }
    }

    suspend fun submitAllDrafts(): Int = withContext(ioDispatcher) {
        var n = 0
        val drafts = dao.getByStatus("draft")
        for (d in drafts) {
            if (submit(d) is SubmitResult.Success) n++
        }
        n
    }
}
