package dev.aliziwa.timesheet

import dev.aliziwa.timesheet.data.ShiftRepository
import dev.aliziwa.timesheet.ui.TimesheetViewModel
import dev.aliziwa.timesheet.util.Utils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimesheetViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var dao: InMemoryShiftDao
    private lateinit var api: FakeTimesheetApi
    private lateinit var repository: ShiftRepository
    private lateinit var viewModel: TimesheetViewModel

    @Before
    fun setUp() {
        dao = InMemoryShiftDao()
        api = FakeTimesheetApi()
        repository = ShiftRepository(dao, api, mainDispatcherRule.dispatcher)
        viewModel = TimesheetViewModel(repository)
    }

    @Test
    fun `initial load fetches shifts into uiState`() = runTest {
        val state = viewModel.uiState.value
        assertFalse(state.loading)
        assertNull(state.message)
    }

    @Test
    fun `createShift validates work order required`() = runTest {
        viewModel.createShift(
            workOrderId = "",
            date = "2026-10-14",
            startHour = 8,
            startMinute = 0,
            endHour = 16,
            endMinute = 30,
            breakMinutes = 30,
            note = null,
            billable = true,
            rateOverride = null,
        )
        assertEquals("Work order required", viewModel.uiState.value.message)
    }

    @Test
    fun `createShift validates bad date`() = runTest {
        viewModel.createShift(
            workOrderId = "WO-101",
            date = "bad-date",
            startHour = 8,
            startMinute = 0,
            endHour = 16,
            endMinute = 30,
            breakMinutes = 30,
            note = null,
            billable = true,
            rateOverride = null,
        )
        assertEquals("Bad date", viewModel.uiState.value.message)
    }

    @Test
    fun `createShift validates end time must be after start time`() = runTest {
        viewModel.createShift(
            workOrderId = "WO-101",
            date = "2026-10-14",
            startHour = 17,
            startMinute = 0,
            endHour = 8,
            endMinute = 0,
            breakMinutes = 0,
            note = null,
            billable = true,
            rateOverride = null,
        )
        assertEquals("End must be after start", viewModel.uiState.value.message)
    }

    @Test
    fun `createShift saves valid shift and calculates weekly totals in uiState`() = runTest {
        val todayDate = Utils.today()
        viewModel.createShift(
            workOrderId = "WO-101",
            date = todayDate,
            startHour = 8,
            startMinute = 0,
            endHour = 18,
            endMinute = 30,
            breakMinutes = 30,
            note = "Site inspection",
            billable = true,
            rateOverride = null,
        )
        val state = viewModel.uiState.value
        assertEquals("Saved", state.message)
        assertEquals(1, state.shifts.size)

        val created = state.shifts[0]
        assertEquals("WO-101", created.workOrderId)
        assertEquals(todayDate, created.date)
        assertEquals(480, created.startMinutes)
        assertEquals(1110, created.endMinutes)
        assertEquals(30, created.breakMinutes)
        assertEquals("draft", created.status)

        // 10 hours worked = 600 min, 120 min overtime
        assertEquals(600, state.workedMinutes)
        assertEquals(120, state.overtimeMinutes)
        assertTrue(state.totalPay > 0.0)
    }

    @Test
    fun `submit updates shift status and server ID on success`() = runTest {
        viewModel.createShift(
            workOrderId = "WO-102",
            date = "2026-10-14",
            startHour = 8,
            startMinute = 0,
            endHour = 16,
            endMinute = 0,
            breakMinutes = 0,
            note = null,
            billable = true,
            rateOverride = null,
        )
        val shift = viewModel.uiState.value.shifts[0]
        viewModel.submit(shift)

        val updatedShift = viewModel.uiState.value.shifts[0]
        assertNotNull(updatedShift.serverId)
        assertEquals("submitted", updatedShift.status)
        assertTrue(viewModel.uiState.value.message?.startsWith("Submitted") == true)
    }

    @Test
    fun `submit sets message on server failure`() = runTest {
        viewModel.createShift(
            workOrderId = "WO-103",
            date = "2026-10-14",
            startHour = 8,
            startMinute = 0,
            endHour = 16,
            endMinute = 0,
            breakMinutes = 0,
            note = null,
            billable = true,
            rateOverride = null,
        )
        val shift = viewModel.uiState.value.shifts[0]
        api.nextFailure = FakeTimesheetApi.httpError(422)

        viewModel.submit(shift)
        assertTrue(viewModel.uiState.value.message?.startsWith("Submit failed") == true)
    }

    @Test
    fun `submitAll submits all draft shifts and updates message`() = runTest {
        viewModel.createShift(
            workOrderId = "WO-105",
            date = "2026-10-14",
            startHour = 8,
            startMinute = 0,
            endHour = 16,
            endMinute = 0,
            breakMinutes = 0,
            note = null,
            billable = true,
            rateOverride = null,
        )
        viewModel.submitAll()
        assertEquals("Submitted 1 shift(s)", viewModel.uiState.value.message)
    }

    @Test
    fun `delete removes shift from list`() = runTest {
        viewModel.createShift(
            workOrderId = "WO-104",
            date = "2026-10-14",
            startHour = 8,
            startMinute = 0,
            endHour = 16,
            endMinute = 0,
            breakMinutes = 0,
            note = null,
            billable = true,
            rateOverride = null,
        )
        val shift = viewModel.uiState.value.shifts[0]
        viewModel.delete(shift)

        assertTrue(viewModel.uiState.value.shifts.isEmpty())
    }

    @Test
    fun `clearMessage clears the current message in uiState`() = runTest {
        viewModel.createShift(
            workOrderId = "",
            date = "2026-10-14",
            startHour = 8,
            startMinute = 0,
            endHour = 16,
            endMinute = 0,
            breakMinutes = 0,
            note = null,
            billable = true,
            rateOverride = null,
        )
        assertNotNull(viewModel.uiState.value.message)
        viewModel.clearMessage()
        assertNull(viewModel.uiState.value.message)
    }
}
