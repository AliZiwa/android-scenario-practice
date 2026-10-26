package dev.aliziwa.timesheet.data

import dev.aliziwa.timesheet.FakeTimesheetApi
import dev.aliziwa.timesheet.InMemoryShiftDao
import dev.aliziwa.timesheet.MainDispatcherRule
import dev.aliziwa.timesheet.data.local.ShiftEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShiftRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var dao: InMemoryShiftDao
    private lateinit var api: FakeTimesheetApi
    private lateinit var repository: ShiftRepository

    @Before
    fun setUp() {
        dao = InMemoryShiftDao()
        api = FakeTimesheetApi()
        repository = ShiftRepository(dao, api, mainDispatcherRule.dispatcher)
    }

    @Test
    fun `getShifts fetches all shifts from DAO`() = runTest {
        val s1 = ShiftEntity(id = 1, workOrderId = "WO-1", date = "2026-10-14")
        val s2 = ShiftEntity(id = 2, workOrderId = "WO-2", date = "2026-10-15")
        dao.insert(s1)
        dao.insert(s2)

        val result = repository.getShifts()
        assertEquals(2, result.size)
    }

    @Test
    fun `save inserts new shift when ID is 0`() = runTest {
        val shift = ShiftEntity(workOrderId = "WO-NEW", date = "2026-10-14")
        val generatedId = repository.save(shift)

        assertTrue(generatedId > 0)
        val fetched = dao.getById(generatedId)
        assertEquals("WO-NEW", fetched?.workOrderId)
    }

    @Test
    fun `save updates existing shift when ID is non-zero`() = runTest {
        val shift = ShiftEntity(workOrderId = "WO-1", date = "2026-10-14")
        val id = repository.save(shift)

        val updated = shift.copy(id = id, workOrderId = "WO-UPDATED")
        repository.save(updated)

        val fetched = dao.getById(id)
        assertEquals("WO-UPDATED", fetched?.workOrderId)
    }

    @Test
    fun `delete removes shift by ID`() = runTest {
        val shift = ShiftEntity(workOrderId = "WO-DEL", date = "2026-10-14")
        val id = repository.save(shift)

        repository.delete(id)
        val fetched = dao.getById(id)
        assertEquals(null, fetched)
    }

    @Test
    fun `submit returns Success and updates DAO on successful response`() = runTest {
        val shift = ShiftEntity(workOrderId = "WO-SUBMIT", date = "2026-10-14", billable = true)
        val id = repository.save(shift)
        shift.id = id

        val result = repository.submit(shift)
        assertTrue(result is SubmitResult.Success)

        val success = result as SubmitResult.Success
        assertEquals("submitted", success.status)
        assertTrue(success.serverId.isNotEmpty())

        val updatedDaoEntity = dao.getById(id)
        assertEquals(success.serverId, updatedDaoEntity?.serverId)
        assertEquals("submitted", updatedDaoEntity?.status)
    }

    @Test
    fun `submit returns Failure on API exception`() = runTest {
        val shift = ShiftEntity(workOrderId = "WO-FAIL", date = "2026-10-14")
        val id = repository.save(shift)
        shift.id = id

        api.nextFailure = FakeTimesheetApi.httpError(422, "{\"error\":\"invalid\"}")

        val result = repository.submit(shift)
        assertTrue(result is SubmitResult.Failure)
    }

    @Test
    fun `submitAllDrafts submits all draft shifts in parallel`() = runTest {
        val d1 = ShiftEntity(workOrderId = "WO-D1", date = "2026-10-14", status = "draft")
        val d2 = ShiftEntity(workOrderId = "WO-D2", date = "2026-10-14", status = "draft")
        val d3 = ShiftEntity(workOrderId = "WO-D3", date = "2026-10-14", status = "submitted")
        repository.save(d1)
        repository.save(d2)
        repository.save(d3)

        val count = repository.submitAllDrafts()
        assertEquals(2, count)

        val remainingDrafts = dao.getByStatus("draft")
        assertTrue(remainingDrafts.isEmpty())
    }
}
