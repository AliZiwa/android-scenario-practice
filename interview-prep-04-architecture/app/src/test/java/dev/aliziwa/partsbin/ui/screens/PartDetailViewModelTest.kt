package dev.aliziwa.partsbin.ui.screens

import app.cash.turbine.test
import dev.aliziwa.partsbin.MainDispatcherRule
import dev.aliziwa.partsbin.domain.AdjustmentError
import dev.aliziwa.partsbin.domain.AdjustmentResult
import dev.aliziwa.partsbin.domain.Part
import dev.aliziwa.partsbin.ui.FakePartsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PartDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakePartsRepository()

    @Test
    fun `load observes part successfully`() = runTest {
        val part = Part("p1", "SKU1", "Name", 10, 5, "Bin", "2026-10-27T14:03:00Z")
        repository.rows.value = mapOf("p1" to part)
        
        val vm = PartDetailViewModel(repository, "p1")

        vm.uiState.test {
            val state = awaitItem()
            assertTrue(state is PartsDetailState.Loaded)
            assertEquals("SKU1", (state as PartsDetailState.Loaded).rowItem.sku)
        }
    }

    @Test
    fun `adjust success updates state`() = runTest {
        val part = Part("p1", "SKU1", "Name", 10, 5, "Bin", "2026-10-27T14:03:00Z")
        repository.rows.value = mapOf("p1" to part)
        repository.adjustResult = AdjustmentResult.Success(part.copy(quantity = 8))
        
        val vm = PartDetailViewModel(repository, "p1")
        
        vm.uiState.test {
            val initialState = awaitItem()
            assertTrue(initialState is PartsDetailState.Loaded)
            assertEquals(10, (initialState as PartsDetailState.Loaded).rowItem.quantity)
            
            vm.adjust(-2, "Used")
            
            val updatedState = awaitItem()
            assertTrue(updatedState is PartsDetailState.Loaded)
            assertEquals(8, (updatedState as PartsDetailState.Loaded).rowItem.quantity)
        }
    }

    @Test
    fun `adjust failure leaves part visible and posts message`() = runTest {
        val part = Part("p1", "SKU1", "Name", 10, 5, "Bin", "2026-10-27T14:03:00Z")
        repository.rows.value = mapOf("p1" to part)
        repository.adjustResult = AdjustmentResult.Rejected(AdjustmentError.REASON_REQUIRED)

        val vm = PartDetailViewModel(repository, "p1")

        vm.uiState.test {
            val state = awaitItem()
            assertTrue(state is PartsDetailState.Loaded)
            
            vm.message.test {
                val initMsg = awaitItem()
                assertEquals(null, initMsg)
                
                vm.adjust(-2, "")
                
                val newMsg = awaitItem()
                assertEquals("Reason required", newMsg)
                
                // Prove the part is still visible
                assertTrue(vm.uiState.value is PartsDetailState.Loaded)
            }
        }
    }
    
    @Test
    fun `adjust failure posts ServerRejected message`() = runTest {
        val part = Part("p1", "SKU1", "Name", 10, 5, "Bin", "2026-10-27T14:03:00Z")
        repository.rows.value = mapOf("p1" to part)
        repository.adjustResult = AdjustmentResult.ServerRejected("insufficient_stock")

        val vm = PartDetailViewModel(repository, "p1")

        vm.uiState.test {
            awaitItem()
            vm.message.test {
                awaitItem()
                vm.adjust(-2, "Used")
                assertEquals("insufficient_stock", awaitItem())
            }
        }
    }

    @Test
    fun `adjust failure posts Offline message`() = runTest {
        val part = Part("p1", "SKU1", "Name", 10, 5, "Bin", "2026-10-27T14:03:00Z")
        repository.rows.value = mapOf("p1" to part)
        repository.adjustResult = AdjustmentResult.Offline

        val vm = PartDetailViewModel(repository, "p1")

        vm.uiState.test {
            awaitItem()
            vm.message.test {
                awaitItem()
                vm.adjust(-2, "Used")
                assertEquals("Offline, adjustment not saved", awaitItem())
            }
        }
    }
    
    @Test
    fun `adjust success posts Stock now N message`() = runTest {
        val part = Part("p1", "SKU1", "Name", 10, 5, "Bin", "2026-10-27T14:03:00Z")
        repository.rows.value = mapOf("p1" to part)
        repository.adjustResult = AdjustmentResult.Success(part.copy(quantity = 8))

        val vm = PartDetailViewModel(repository, "p1")

        vm.uiState.test {
            awaitItem()
            vm.message.test {
                awaitItem()
                vm.adjust(-2, "Used")
                assertEquals("Stock now 8", awaitItem())
            }
            awaitItem() // Consume updated state
        }
    }
}
