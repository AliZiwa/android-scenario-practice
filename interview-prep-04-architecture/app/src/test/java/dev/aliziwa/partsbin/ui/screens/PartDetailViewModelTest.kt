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
}
