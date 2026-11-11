package dev.aliziwa.partsbin.data.remote

import dev.aliziwa.partsbin.InMemoryPartDao
import dev.aliziwa.partsbin.domain.AdjustmentError
import dev.aliziwa.partsbin.domain.AdjustmentResult
import dev.aliziwa.partsbin.data.local.db.PartEntity
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class DefaultPartsRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var dao: InMemoryPartDao
    private lateinit var repository: DefaultPartsRepository

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        
        dao = InMemoryPartDao()
        repository = DefaultPartsRepository(
            partsService = retrofit.create(PartsService::class.java),
            partDao = dao
        )
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `refreshParts network failure leaves Room unchanged`() = runTest {
        mockWebServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        
        dao.upsert(PartEntity("p-01", "SKU1", "Name", 10, 5, "Bin", 1, "2026-10-27T14:03:00Z"))
        
        val result = repository.refreshParts()
        assertTrue(result is dev.aliziwa.partsbin.domain.RefreshResult.Offline)

        assertEquals(1, dao.getAll().size)
    }

    @Test
    fun `refreshParts success upserts to Room`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""[{"id":"p-01","sku":"SKU1","name":"Name","quantity":10,"minQuantity":5,"binLocation":"Bin","version":1,"updatedAt":"2026-10-27T14:03:00Z"}]""")
        )

        repository.refreshParts()

        assertEquals(1, dao.getAll().size)
    }

    @Test
    fun `adjust success writes through to Room`() = runTest {
        dao.upsert(PartEntity("p-01", "SKU1", "Name", 10, 5, "Bin", 1, "2026-10-27T14:03:00Z"))
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"id":"p-01","sku":"SKU1","name":"Name","quantity":8,"minQuantity":5,"binLocation":"Bin","version":2,"updatedAt":"2026-10-27T14:04:00Z"}""")
        )

        val result = repository.adjust("p-01", -2, "Used")
        
        assertTrue(result is AdjustmentResult.Success)
        val success = result as AdjustmentResult.Success
        assertEquals(8, success.part.quantity)
        assertEquals(8, dao.getById("p-01")?.quantity)
    }

    @Test
    fun `adjust server 422 leaves Room alone`() = runTest {
        dao.upsert(PartEntity("p-01", "SKU1", "Name", 10, 5, "Bin", 1, "2026-10-27T14:03:00Z"))
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(422)
                .setBody("""{"error":"insufficient_stock"}""")
        )

        val result = repository.adjust("p-01", -2, "Used")
        
        assertTrue(result is AdjustmentResult.ServerRejected)
        val serverRejected = result as AdjustmentResult.ServerRejected
        assertEquals("insufficient_stock", serverRejected.message)
        assertEquals(10, dao.getById("p-01")?.quantity)
    }

    @Test
    fun `adjust IOException leaves Room alone`() = runTest {
        dao.upsert(PartEntity("p-01", "SKU1", "Name", 10, 5, "Bin", 1, "2026-10-27T14:03:00Z"))
        
        mockWebServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val result = repository.adjust("p-01", -2, "Used")
        
        assertTrue(result is AdjustmentResult.Offline)
        assertEquals(10, dao.getById("p-01")?.quantity)
    }

    @Test
    fun `adjust blank reason rejected before network`() = runTest {
        dao.upsert(PartEntity("p-01", "SKU1", "Name", 10, 5, "Bin", 1, "2026-10-27T14:03:00Z"))

        val result = repository.adjust("p-01", -2, "")
        
        assertTrue(result is AdjustmentResult.Rejected)
        assertEquals(AdjustmentError.REASON_REQUIRED, (result as AdjustmentResult.Rejected).reason)
        assertEquals(0, mockWebServer.requestCount)
    }

    @Test
    fun `adjust negative quantity rejected before network`() = runTest {
        dao.upsert(PartEntity("p-01", "SKU1", "Name", 10, 5, "Bin", 1, "2026-10-27T14:03:00Z"))

        val result = repository.adjust("p-01", -15, "Used")
        
        assertTrue(result is AdjustmentResult.Rejected)
        assertEquals(AdjustmentError.INSUFFICIENT_STOCK, (result as AdjustmentResult.Rejected).reason)
        assertEquals(0, mockWebServer.requestCount)
    }
}
