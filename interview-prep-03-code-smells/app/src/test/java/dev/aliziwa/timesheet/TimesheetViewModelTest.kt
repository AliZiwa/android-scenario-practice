package dev.aliziwa.timesheet

import dev.aliziwa.timesheet.data.RateProvider
import dev.aliziwa.timesheet.data.ShiftRepository
import dev.aliziwa.timesheet.data.local.ShiftEntity
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
    private lateinit var rateProvider: RateProvider
    private lateinit var viewModel: TimesheetViewModel

    @Before
    fun setUp() {
        dao = InMemoryShiftDao()
        api = FakeTimesheetApi()
        repository = ShiftRepository(dao, api, mainDispatcherRule.dispatcher)
        rateProvider = RateProvider { 42.0 }
        viewModel = TimesheetViewModel(repository, rateProvider)
    }

    @Test
    fun `initial load fetches pre-populated shifts in newest-first order into uiState`() = runTest {
        val olderShift = ShiftEntity(id = 1, workOrderId = "WO-OLD", date = "2026-10-12", startMinutes = 8 * 60)
        val newerShift = ShiftEntity(id = 2, workOrderId = "WO-NEW", date = "2026-10-14", startMinutes = 8 * 60)
        dao.insert(olderShift)
        dao.insert(newerShift)

        val vm = TimesheetViewModel(repository, rateProvider)
        val state = vm.uiState.value

        assertFalse(state.loading)
        assertNull(state.message)
        assertEquals(2, state.shifts.size)
        // Newest date first
        assertEquals("WO-NEW", state.shifts[0].workOrderId)
        assertEquals("WO-OLD", state.shifts[1].workOrderId)
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
    fun `createShift with custom rate provider calculates exact pay number in uiState`() = runTest {
        val customRateProvider = RateProvider { 55.0 } // Technician with hourly_rate = 55
        val vm = TimesheetViewModel(repository, customRateProvider)
        val todayDate = Utils.today()

        // 10 hours worked = 8h standard ($55) + 2h overtime ($82.50) = $440 + $165 = $605.00
        vm.createShift(
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

        val state = vm.uiState.value
        assertEquals("Saved", state.message)
        assertEquals(1, state.shifts.size)
        assertEquals(600, state.workedMinutes)
        assertEquals(120, state.overtimeMinutes)
        assertEquals(605.0, state.totalPay, 0.001)
    }

    @Test
    fun `submit billable shift updates status to submitted`() = runTest {
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
    fun `submit unbillable shift updates status to approved`() = runTest {
        viewModel.createShift(
            workOrderId = "WO-UNBILLABLE",
            date = "2026-10-14",
            startHour = 8,
            startMinute = 0,
            endHour = 16,
            endMinute = 0,
            breakMinutes = 0,
            note = "Internal training",
            billable = false,
            rateOverride = null,
        )
        val shift = viewModel.uiState.value.shifts[0]
        viewModel.submit(shift)

        val updatedShift = viewModel.uiState.value.shifts[0]
        assertNotNull(updatedShift.serverId)
        assertEquals("approved", updatedShift.status)
    }

    @Test
    fun `submit already submitted shift displays Already submitted error`() = runTest {
        val submittedShift = ShiftEntity(
            id = 1,
            workOrderId = "WO-DONE",
            date = "2026-10-14",
            status = "submitted",
            serverId = "srv-999",
        )
        dao.insert(submittedShift)
        viewModel.load()

        val shift = viewModel.uiState.value.shifts[0]
        viewModel.submit(shift)

        assertEquals("Already submitted", viewModel.uiState.value.message)
        assertEquals(0, api.submitted.size) // No HTTP request made
    }

    @Test
    fun `submit failure sets message and leaves shift as draft`() = runTest {
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
        val currentShift = viewModel.uiState.value.shifts[0]
        assertEquals("draft", currentShift.status)
        assertNull(currentShift.serverId)
    }

    @Test
    fun `submitAll submits all draft shifts sequentially and updates message`() = runTest {
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
