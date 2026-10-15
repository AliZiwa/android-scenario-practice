package dev.aliziwa.dispatch.data

import dev.aliziwa.dispatch.InMemoryWorkOrderDao
import dev.aliziwa.dispatch.data.local.WorkOrderEntity
import dev.aliziwa.dispatch.data.remote.WorkOrdersApi
import dev.aliziwa.dispatch.domain.Priority
import dev.aliziwa.dispatch.domain.Status
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Repository tests against a real Retrofit stack and a MockWebServer. The DAO is in-memory.
 * Extend this file for the PATCH paths: success, IOException, 409, 400.
 */
class DefaultWorkOrdersRepositoryTest {

    private val server = MockWebServer()
    private val dao = InMemoryWorkOrderDao()
    private lateinit var repository: DefaultWorkOrdersRepository

    @Before
    fun setUp() {
        server.start()
        val client = OkHttpClient.Builder().build()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(WorkOrdersApi::class.java)
        repository = DefaultWorkOrdersRepository(dao = dao, api = api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `refresh replaces the cache with what the server returned`() = runTest {
        dao.upsert(entity("wo-stale", status = "DONE", updatedAt = 1))
        server.enqueue(jsonResponse(200, TWO_ORDERS))

        val count = repository.refresh()

        assertEquals(2, count)
        val cached = dao.getAllOnce().map { it.id }.sorted()
        assertEquals(listOf("wo-101", "wo-102"), cached)
        assertEquals("GET /work-orders", server.takeRequest().let { "${it.method} ${it.path}" })
    }

    @Test
    fun `observeWorkOrders sorts by priority then recency`() = runTest {
        dao.upsertAll(
            listOf(
                entity("low-new", priority = "LOW", updatedAt = 30),
                entity("high-old", priority = "HIGH", updatedAt = 10),
                entity("high-new", priority = "HIGH", updatedAt = 20),
                entity("medium", priority = "MEDIUM", updatedAt = 40),
            ),
        )

        val ids = repository.observeWorkOrders().first().map { it.id }

        assertEquals(listOf("high-new", "high-old", "medium", "low-new"), ids)
    }

    @Test
    fun `entities map to domain enums`() = runTest {
        dao.upsert(entity("wo-1", priority = "MEDIUM", status = "IN_PROGRESS"))

        val order = repository.observeWorkOrder("wo-1").first()!!

        assertEquals(Priority.MEDIUM, order.priority)
        assertEquals(Status.IN_PROGRESS, order.status)
    }

    private fun jsonResponse(code: Int, body: String): MockResponse =
        MockResponse()
            .setResponseCode(code)
            .setHeader("Content-Type", "application/json")
            .setBody(body)

    private fun entity(
        id: String,
        priority: String = "HIGH",
        status: String = "OPEN",
        version: Int = 1,
        updatedAt: Long = 0,
    ) = WorkOrderEntity(
        id = id,
        title = "Title $id",
        site = "Bay 1",
        priority = priority,
        status = status,
        version = version,
        updatedAt = updatedAt,
    )

    private companion object {
        val TWO_ORDERS = """
            [
              {"id":"wo-101","title":"Replace hydraulic seal","site":"Bay 3","priority":"HIGH","status":"OPEN","version":1,"updatedAt":1760400000000},
              {"id":"wo-102","title":"TPMS reset","site":"Bay 1","priority":"MEDIUM","status":"OPEN","version":1,"updatedAt":1760400600000}
            ]
        """.trimIndent()
    }
}
