package dev.aliziwa.timesheet.data

import android.content.Context
import android.util.Log
import android.widget.Toast
import dev.aliziwa.timesheet.Injector
import dev.aliziwa.timesheet.data.local.ShiftEntity
import dev.aliziwa.timesheet.data.remote.ShiftDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ShiftRepository(private val context: Context) {

    var lastError: String? = null
    var submitCount = 0

    suspend fun getShifts(): List<ShiftEntity> {
        return withContext(Dispatchers.IO) {
            Injector.db.shiftDao().getAll()
        }
    }

    suspend fun save(shift: ShiftEntity): Long {
        return withContext(Dispatchers.IO) {
            if (shift.id == 0L) {
                Injector.db.shiftDao().insert(shift)
            } else {
                Injector.db.shiftDao().update(shift)
                shift.id
            }
        }
    }

    suspend fun delete(id: Long) {
        withContext(Dispatchers.IO) {
            Injector.db.shiftDao().deleteById(id)
        }
    }

    /**
     * Submits a shift. Returns true if the server accepted it. On success the shift is updated in place
     * and saved.
     */
    suspend fun submit(shift: ShiftEntity): Boolean {
        return withContext(Dispatchers.IO) {
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
                val resp = Injector.api.submit(dto)
                shift.serverId = resp.serverId
                shift.status = resp.status
                Injector.db.shiftDao().update(shift)
                submitCount++
                lastError = null
                true
            } catch (e: Exception) {
                Log.e("ShiftRepository", "submit failed", e)
                lastError = e.message
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Submit failed", Toast.LENGTH_SHORT).show()
                }
                false
            }
        }
    }

    suspend fun submitAllDrafts(): Int {
        var n = 0
        val drafts = withContext(Dispatchers.IO) { Injector.db.shiftDao().getByStatus("draft") }
        for (d in drafts) {
            if (submit(d)) n++
        }
        return n
    }
}
