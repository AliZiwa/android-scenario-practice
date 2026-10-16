package dev.aliziwa.timesheet.util

import dev.aliziwa.timesheet.data.local.ShiftEntity
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Two characterization tests to start from. They describe what the code does today, not what it should do.
 * Add the rest before you refactor anything; every one of them must pass on `main` unchanged.
 */
class UtilsTest {

    @Test
    fun `worked minutes rounds to the nearest quarter hour`() {
        assertEquals(480, Utils.workedMinutes(shift(start = 8 * 60, end = 16 * 60 + 30, breakMinutes = 30)))
        assertEquals(495, Utils.workedMinutes(shift(start = 8 * 60, end = 16 * 60 + 38, breakMinutes = 30)))
        assertEquals(480, Utils.workedMinutes(shift(start = 8 * 60, end = 16 * 60 + 37, breakMinutes = 30)))
        assertEquals(0, Utils.workedMinutes(shift(start = 9 * 60, end = 9 * 60 + 10, breakMinutes = 30)))
    }

    @Test
    fun `pay is time and a half past eight hours and zero when unbillable`() {
        val tenHours = shift(start = 7 * 60, end = 17 * 60, breakMinutes = 0)
        assertEquals(8 * 40.0 + 2 * 60.0, Utils.calcPay(tenHours, rate = 40.0), 0.001)
        assertEquals(0.0, Utils.calcPay(tenHours.copy(billable = false), rate = 40.0), 0.001)
        assertEquals(8 * 50.0 + 2 * 75.0, Utils.calcPay(tenHours.copy(rateOverride = 50.0), rate = 40.0), 0.001)
    }

    private fun shift(start: Int, end: Int, breakMinutes: Int) = ShiftEntity(
        workOrderId = "wo-1",
        date = "2026-10-14",
        startMinutes = start,
        endMinutes = end,
        breakMinutes = breakMinutes,
    )
}
