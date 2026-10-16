package dev.aliziwa.timesheet.util

import dev.aliziwa.timesheet.Injector
import dev.aliziwa.timesheet.data.local.ShiftEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object Utils {

    val DATE_FMT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val DISPLAY_FMT = SimpleDateFormat("EEE d MMM", Locale.US)

    /** Worked minutes, rounded to the nearest quarter hour, never negative. */
    fun workedMinutes(shift: ShiftEntity): Int {
        var m = shift.endMinutes - shift.startMinutes - shift.breakMinutes
        if (m < 0) m = 0
        // round to nearest 15
        val r = m % 15
        if (r >= 8) m = m + (15 - r) else m = m - r
        return m
    }

    /** Pay for one shift. Overtime is anything past 8 hours in the shift, at 1.5x. Unbillable shifts pay 0. */
    fun calcPay(shift: ShiftEntity, rate: Double): Double {
        if (!shift.billable) return 0.0
        val r = if (shift.rateOverride != null) shift.rateOverride!! else rate
        val m = workedMinutes(shift)
        var pay = 0.0
        if (m > 480) {
            pay = 480 / 60.0 * r + (m - 480) / 60.0 * r * 1.5
        } else {
            pay = m / 60.0 * r
        }
        return Math.round(pay * 100) / 100.0
    }

    fun fmtDuration(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (m == 0) "${h}h" else "${h}h ${m}m"
    }

    fun fmtMoney(amount: Double): String = String.format(Locale.US, "$%.2f", amount)

    fun fmtTime(minutes: Int): String = String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60)

    /** Epoch millis for a yyyy-MM-dd string, or -1 if it does not parse. */
    fun parseDate(s: String): Long {
        return try {
            DATE_FMT.parse(s)?.time ?: -1
        } catch (e: Exception) {
            -1
        }
    }

    fun displayDate(s: String): String {
        val t = parseDate(s)
        if (t == -1L) return s
        return DISPLAY_FMT.format(t)
    }

    /** Monday of the week containing the date, as yyyy-MM-dd. Weeks start on Monday. */
    fun weekStart(date: String): String {
        val t = parseDate(date)
        if (t == -1L) return date
        val c = Calendar.getInstance(Locale.US)
        c.timeInMillis = t
        c.firstDayOfWeek = Calendar.MONDAY
        var dow = c.get(Calendar.DAY_OF_WEEK) // 1 = Sunday
        if (dow == 1) dow = 8
        c.add(Calendar.DAY_OF_MONTH, -(dow - 2))
        return DATE_FMT.format(c.time)
    }

    fun isSubmitted(shift: ShiftEntity): Boolean {
        return shift.status.equals("submitted", ignoreCase = true) || shift.status.equals("approved", ignoreCase = true)
    }

    fun statusLabel(status: String): String {
        if (status == "draft") return "Draft"
        if (status == "submitted") return "Submitted"
        if (status == "approved") return "Approved"
        return status
    }

    fun rate(): Double {
        val prefs = Injector.context.getSharedPreferences("timesheet", 0)
        return prefs.getFloat("hourly_rate", 42.0f).toDouble()
    }

    fun today(): String = DATE_FMT.format(System.currentTimeMillis())
}
