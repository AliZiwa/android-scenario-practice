package dev.aliziwa.timesheet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aliziwa.timesheet.Injector
import dev.aliziwa.timesheet.data.local.ShiftEntity
import dev.aliziwa.timesheet.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TimesheetViewModel : ViewModel() {

    val shifts = MutableStateFlow<MutableList<ShiftEntity>>(mutableListOf())
    val loading = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)
    var lastError: String? = null
    var selectedWeek: String = Utils.weekStart(Utils.today())
    var data2: List<ShiftEntity> = emptyList() // cache for summary

    init {
        load(false, false)
    }

    fun load(forceRefresh: Boolean, silent: Boolean) {
        if (!silent) loading.value = true
        viewModelScope.launch {
            try {
                val list = if (forceRefresh || data2.isEmpty()) {
                    Injector.repository.getShifts()
                } else {
                    data2
                }
                data2 = list
                shifts.value.clear()
                shifts.value.addAll(list)
                shifts.value = shifts.value.toMutableList() // force an emission
                lastError = null
            } catch (e: Exception) {
                lastError = e.message
                message.value = "Could not load shifts"
            }
            if (!silent) loading.value = false
        }
    }

    fun createShift(
        workOrderId: String,
        date: String,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        breakMinutes: Int,
        note: String?,
        billable: Boolean,
        rateOverride: Double?,
    ) {
        if (workOrderId.isBlank()) {
            message.value = "Work order required"
            return
        }
        if (Utils.parseDate(date) == -1L) {
            message.value = "Bad date"
            return
        }
        val s = ShiftEntity()
        s.workOrderId = workOrderId.trim()
        s.date = date
        s.startMinutes = startHour * 60 + startMinute
        s.endMinutes = endHour * 60 + endMinute
        s.breakMinutes = breakMinutes
        s.note = note
        s.billable = billable
        s.rateOverride = rateOverride
        s.status = "draft"
        if (s.endMinutes <= s.startMinutes) {
            message.value = "End must be after start"
            return
        }
        viewModelScope.launch {
            val id = withContext(Dispatchers.IO) { Injector.repository.save(s) }
            s.id = id
            shifts.value.add(0, s)
            shifts.value = shifts.value.toMutableList() // force an emission
            data2 = shifts.value.toList()
            message.value = "Saved"
        }
    }

    fun submit(shift: ShiftEntity) {
        if (Utils.isSubmitted(shift)) {
            message.value = "Already submitted"
            return
        }
        loading.value = true
        viewModelScope.launch {
            val ok = Injector.repository.submit(shift)
            if (ok) {
                message.value = "Submitted " + shift.serverId
            } else {
                message.value = "Submit failed: " + Injector.repository.lastError
            }
            load(true, true)
            loading.value = false
        }
    }

    fun submitAll() {
        loading.value = true
        viewModelScope.launch {
            val n = Injector.repository.submitAllDrafts()
            message.value = "Submitted $n shift(s)"
            load(true, true)
            loading.value = false
        }
    }

    fun delete(shift: ShiftEntity) {
        viewModelScope.launch {
            Injector.repository.delete(shift.id)
            shifts.value.remove(shift)
            shifts.value = shifts.value.toMutableList() // force an emission
            data2 = shifts.value.toList()
        }
    }

    /** Totals for the selected week: worked minutes, overtime minutes, pay. */
    fun weekSummary(): Triple<Int, Int, Double> {
        var worked = 0
        var ot = 0
        var pay = 0.0
        val rate = Utils.rate()
        for (s in data2) {
            if (Utils.weekStart(s.date) == selectedWeek) {
                val m = Utils.workedMinutes(s)
                worked += m
                if (m > 480) ot += m - 480
                pay += Utils.calcPay(s, rate)
            }
        }
        return Triple(worked, ot, Math.round(pay * 100) / 100.0)
    }

    fun formatRow(shift: ShiftEntity): String {
        val sb = StringBuilder()
        sb.append(Utils.displayDate(shift.date))
        sb.append("  ")
        sb.append(Utils.fmtTime(shift.startMinutes))
        sb.append(" to ")
        sb.append(Utils.fmtTime(shift.endMinutes))
        if (shift.breakMinutes > 0) {
            sb.append(" (")
            sb.append(shift.breakMinutes)
            sb.append("m break)")
        }
        sb.append("  ")
        sb.append(Utils.fmtDuration(Utils.workedMinutes(shift)))
        if (!shift.billable) sb.append("  unbillable")
        return sb.toString()
    }

    fun clearMessage() {
        message.value = null
    }
}
