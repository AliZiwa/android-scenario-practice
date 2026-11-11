package dev.aliziwa.partsbin.data.remote

import dev.aliziwa.partsbin.InMemoryPartDao
import dev.aliziwa.partsbin.data.local.db.PartEntity
import dev.aliziwa.partsbin.domain.ServerException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import java.io.IOException

class DefaultPartsRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var partsService: PartsService
    private lateinit var inMemoryPartDao: InMemoryPartDao
    private lateinit var repository: DefaultPartsRepository

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        partsService = retrofit.create(PartsService::class.java)
        inMemoryPartDao = InMemoryPartDao()
        repository = DefaultPartsRepository(partsService, inMemoryPartDao)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `refreshParts success upserts remote parts`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""[{"id":"1", "sku":"SKU1", "name":"Part 1", "quantity":10, "minQuantity":5, "binLocation":"A1", "version":1, "updatedAt":"2026-10-27T14:03:00Z"}]""")
                .setResponseCode(200)
        )

        repository.refreshParts()

        val saved = inMemoryPartDao.getAll()
        assertEquals(1, saved.size)
        assertEquals("1", saved[0].id)
        assertEquals("SKU1", saved[0].sku)
    }

    @Test
    fun `refreshParts network failure leaves Dao unchanged`() = runTest {
        inMemoryPartDao.upsert(PartEntity("1", "SKU1", "Old", 10, 5, "A1", 1, "2026-10-27T14:03:00Z"))
        
        mockWebServer.enqueue(MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START))

        try {
            repository.refreshParts()
        } catch (e: Exception) {
            assertTrue(e is IOException)
        }

        val saved = inMemoryPartDao.getAll()
        assertEquals(1, saved.size)
        assertEquals("Old", saved[0].name)
    }

    @Test
    fun `adjust success upserts and returns updated part`() = runTest {
        inMemoryPartDao.upsert(PartEntity("1", "SKU1", "Part 1", 10, 5, "A1", 1, "2026-10-27T14:03:00Z"))

        mockWebServer.enqueue(
            MockResponse()
                .setBody("""{"id":"1", "sku":"SKU1", "name":"Part 1", "quantity":8, "minQuantity":5, "binLocation":"A1", "version":2, "updatedAt":"2026-10-27T15:03:00Z"}""")
                .setResponseCode(200)
        )

        val result = repository.adjust("1", -2, "Used")
        assertEquals(8, result.quantity)

        val saved = inMemoryPartDao.getById("1")
        assertEquals(8, saved?.quantity)
        assertEquals(2, saved?.version)
    }

    @Test
    fun `adjust server 422 throws ServerException and leaves Dao unchanged`() = runTest {
        inMemoryPartDao.upsert(PartEntity("1", "SKU1", "Part 1", 10, 5, "A1", 1, "2026-10-27T14:03:00Z"))

        mockWebServer.enqueue(
            MockResponse()
                .setBody("""{"error":"insufficient_stock"}""")
                .setHeader("Content-Type", "application/json")
                .setResponseCode(422)
        )

        var exception: ServerException? = null
        try {
            repository.adjust("1", -2, "Used")
        } catch (e: ServerException) {
            exception = e
        }

        assertTrue(exception != null)
        assertTrue(exception?.message == "insufficient_stock" || exception?.message == "Server said 422")

        val saved = inMemoryPartDao.getById("1")
        assertEquals(10, saved?.quantity)
    }

    @Test
    fun `adjust IO error leaves Dao unchanged`() = runTest {
        inMemoryPartDao.upsert(PartEntity("1", "SKU1", "Part 1", 10, 5, "A1", 1, "2026-10-27T14:03:00Z"))

        mockWebServer.enqueue(MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START))

        try {
            repository.adjust("1", -2, "Used")
        } catch (e: Exception) {
            assertTrue(e is IOException)
        }

        val saved = inMemoryPartDao.getById("1")
        assertEquals(10, saved?.quantity)
    }

    @Test
    fun `adjust negative delta throws exception`() = runTest {
        inMemoryPartDao.upsert(PartEntity("1", "SKU1", "Part 1", 2, 5, "A1", 1, "2026-10-27T14:03:00Z"))

        var exception: IllegalArgumentException? = null
        try {
            repository.adjust("1", -3, "Used")
        } catch (e: IllegalArgumentException) {
            exception = e
        }

        assertTrue(exception != null)
        assertEquals("Not enough stock", exception?.message)
    }

    @Test
    fun `adjust blank reason throws exception`() = runTest {
        inMemoryPartDao.upsert(PartEntity("1", "SKU1", "Part 1", 10, 5, "A1", 1, "2026-10-27T14:03:00Z"))

        var exception: IllegalArgumentException? = null
        try {
            repository.adjust("1", -2, "   ")
        } catch (e: IllegalArgumentException) {
            exception = e
        }

        assertTrue(exception != null)
        assertEquals("Reason required", exception?.message)
    }
}
