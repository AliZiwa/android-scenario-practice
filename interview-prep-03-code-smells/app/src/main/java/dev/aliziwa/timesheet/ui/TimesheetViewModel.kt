package dev.aliziwa.timesheet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aliziwa.timesheet.Injector
import dev.aliziwa.timesheet.data.RateProvider
import dev.aliziwa.timesheet.data.ShiftRepository
import dev.aliziwa.timesheet.data.SubmitResult
import dev.aliziwa.timesheet.data.local.ShiftEntity
import dev.aliziwa.timesheet.util.Utils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShiftUiModel(
    val shift: ShiftEntity,
    val pay: Double,
    val formattedRow: String
)

data class TimesheetUiState(
    val shiftModels: List<ShiftUiModel> = emptyList(),
    val loading: Boolean = false,
    val message: String? = null,
    val selectedWeek: String = Utils.weekStart(Utils.today()),
    val workedMinutes: Int = 0,
    val overtimeMinutes: Int = 0,
    val totalPay: Double = 0.0,
)

class TimesheetViewModel(
    private val repository: ShiftRepository = Injector.repository,
    private val rateProvider: RateProvider = Injector.rateProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimesheetUiState())
    val uiState: StateFlow<TimesheetUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true) }
        viewModelScope.launch {
            try {
                val list = repository.getShifts()
                updateShifts(list)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(message = "Could not load shifts")
                }
            } finally {
                _uiState.update { it.copy(loading = false) }
            }
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
            _uiState.update { it.copy(message = "Work order required") }
            return
        }
        if (Utils.parseDate(date) == -1L) {
            _uiState.update { it.copy(message = "Bad date") }
            return
        }
        val startMinutes = startHour * 60 + startMinute
        val endMinutes = endHour * 60 + endMinute
        if (endMinutes <= startMinutes) {
            _uiState.update { it.copy(message = "End must be after start") }
            return
        }

        val s = ShiftEntity(
            workOrderId = workOrderId.trim(),
            date = date,
            startMinutes = startMinutes,
            endMinutes = endMinutes,
            breakMinutes = breakMinutes,
            note = note,
            billable = billable,
            rateOverride = rateOverride,
            status = "draft",
        )

        viewModelScope.launch {
            val id = repository.save(s)
            s.id = id
            val currentList = _uiState.value.shiftModels.map { it.shift }.toMutableList()
            currentList.add(0, s)
            updateShifts(currentList)
            _uiState.update { it.copy(message = "Saved") }
        }
    }

    fun submit(shift: ShiftEntity) {
        if (Utils.isSubmitted(shift)) {
            _uiState.update { it.copy(message = "Already submitted") }
            return
        }
        _uiState.update { it.copy(loading = true) }
        viewModelScope.launch {
            val result = repository.submit(shift)
            val msg = when (result) {
                is SubmitResult.Success -> "Submitted " + result.serverId
                is SubmitResult.Failure -> "Submit failed: " + result.reason
            }
            val list = repository.getShifts()
            updateShifts(list)
            _uiState.update { it.copy(message = msg, loading = false) }
        }
    }

    fun submitAll() {
        _uiState.update { it.copy(loading = true) }
        viewModelScope.launch {
            val n = repository.submitAllDrafts()
            val list = repository.getShifts()
            updateShifts(list)
            _uiState.update { it.copy(message = "Submitted $n shift(s)", loading = false) }
        }
    }

    fun delete(shift: ShiftEntity) {
        viewModelScope.launch {
            repository.delete(shift.id)
            val updated = _uiState.value.shiftModels.map { it.shift }.filterNot { it.id == shift.id }
            updateShifts(updated)
        }
    }

    private fun updateShifts(newList: List<ShiftEntity>) {
        val week = _uiState.value.selectedWeek
        var worked = 0
        var ot = 0
        var pay = 0.0
        val rate = rateProvider.getRate()

        val shiftModels = newList.map { s ->
            val shiftPay = Utils.calcPay(s, rate)
            if (Utils.weekStart(s.date) == week) {
                val m = Utils.workedMinutes(s)
                worked += m
                if (m > 480) ot += m - 480
                pay += shiftPay
            }
            ShiftUiModel(shift = s, pay = shiftPay, formattedRow = formatRow(s))
        }

        pay = Math.round(pay * 100) / 100.0

        _uiState.update {
            it.copy(
                shiftModels = shiftModels,
                workedMinutes = worked,
                overtimeMinutes = ot,
                totalPay = pay,
            )
        }
    }

    private fun formatRow(shift: ShiftEntity): String {
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
        _uiState.update { it.copy(message = null) }
    }
}
