package dev.aliziwa.timesheet.util

import dev.aliziwa.timesheet.data.local.ShiftEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Characterization tests describing current application behavior.
 */
class UtilsTest {

    @Test
    fun `worked minutes rounds to the nearest quarter hour`() {
        assertEquals(480, Utils.workedMinutes(shift(start = 8 * 60, end = 16 * 60 + 30, breakMinutes = 30)))
        assertEquals(495, Utils.workedMinutes(shift(start = 8 * 60, end = 16 * 60 + 38, breakMinutes = 30)))
        assertEquals(480, Utils.workedMinutes(shift(start = 8 * 60, end = 16 * 60 + 37, breakMinutes = 30)))
        assertEquals(0, Utils.workedMinutes(shift(start = 9 * 60, end = 9 * 60 + 10, breakMinutes = 30)))
        // Clamps to 0 when end - start - break is negative
        assertEquals(0, Utils.workedMinutes(shift(start = 10 * 60, end = 9 * 60, breakMinutes = 30)))
    }

    @Test
    fun `pay is time and a half past eight hours and zero when unbillable`() {
        val tenHours = shift(start = 7 * 60, end = 17 * 60, breakMinutes = 0)
        assertEquals(8 * 40.0 + 2 * 60.0, Utils.calcPay(tenHours, rate = 40.0), 0.001)
        assertEquals(0.0, Utils.calcPay(tenHours.copy(billable = false), rate = 40.0), 0.001)
        assertEquals(8 * 50.0 + 2 * 75.0, Utils.calcPay(tenHours.copy(rateOverride = 50.0), rate = 40.0), 0.001)

        val fourHours = shift(start = 8 * 60, end = 12 * 60, breakMinutes = 0)
        assertEquals(4 * 40.0, Utils.calcPay(fourHours, rate = 40.0), 0.001)
    }

    @Test
    fun `fmtDuration formats hours and minutes correctly`() {
        assertEquals("8h", Utils.fmtDuration(480))
        assertEquals("8h 30m", Utils.fmtDuration(510))
        assertEquals("0h 15m", Utils.fmtDuration(15))
    }

    @Test
    fun `fmtMoney formats currency amounts`() {
        assertEquals("$42.00", Utils.fmtMoney(42.0))
        assertEquals("$100.50", Utils.fmtMoney(100.5))
        assertEquals("$0.00", Utils.fmtMoney(0.0))
    }

    @Test
    fun `fmtTime formats minutes past midnight`() {
        assertEquals("08:00", Utils.fmtTime(8 * 60))
        assertEquals("16:30", Utils.fmtTime(16 * 60 + 30))
        assertEquals("00:05", Utils.fmtTime(5))
    }

    @Test
    fun `parseDate handles valid and invalid date strings`() {
        assertTrue(Utils.parseDate("2026-10-14") > 0)
        assertEquals(-1L, Utils.parseDate("invalid-date"))
    }

    @Test
    fun `displayDate formats valid dates and returns raw input for invalid dates`() {
        assertEquals("Wed 14 Oct", Utils.displayDate("2026-10-14"))
        assertEquals("invalid-date", Utils.displayDate("invalid-date"))
    }

    @Test
    fun `weekStart calculates Monday for given dates`() {
        // 2026-10-14 is Wednesday -> Monday is 2026-10-12
        assertEquals("2026-10-12", Utils.weekStart("2026-10-14"))
        // 2026-10-12 is Monday -> Monday is 2026-10-12
        assertEquals("2026-10-12", Utils.weekStart("2026-10-12"))
        // 2026-10-18 is Sunday -> Monday is 2026-10-12
        assertEquals("2026-10-12", Utils.weekStart("2026-10-18"))
        // Invalid date returns original input
        assertEquals("bad-date", Utils.weekStart("bad-date"))
    }

    @Test
    fun `isSubmitted returns true for submitted or approved statuses`() {
        assertTrue(Utils.isSubmitted(ShiftEntity(status = "submitted")))
        assertTrue(Utils.isSubmitted(ShiftEntity(status = "approved")))
        assertTrue(Utils.isSubmitted(ShiftEntity(status = "SUBMITTED")))
        assertFalse(Utils.isSubmitted(ShiftEntity(status = "draft")))
    }

    @Test
    fun `statusLabel formats labels properly`() {
        assertEquals("Draft", Utils.statusLabel("draft"))
        assertEquals("Submitted", Utils.statusLabel("submitted"))
        assertEquals("Approved", Utils.statusLabel("approved"))
        assertEquals("other", Utils.statusLabel("other"))
    }

    private fun shift(start: Int, end: Int, breakMinutes: Int) = ShiftEntity(
        workOrderId = "wo-1",
        date = "2026-10-14",
        startMinutes = start,
        endMinutes = end,
        breakMinutes = breakMinutes,
    )
}
