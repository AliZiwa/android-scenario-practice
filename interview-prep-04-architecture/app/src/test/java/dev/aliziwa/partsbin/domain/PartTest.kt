package dev.aliziwa.partsbin.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PartTest {

    @Test
    fun `lowStock is true when quantity equals minQuantity`() {
        val part = Part("p1", "SKU", "Name", quantity = 5, minQuantity = 5, "Bin", "2026-10-27")
        assertTrue(part.lowStock)
    }

    @Test
    fun `lowStock is true when quantity is below minQuantity`() {
        val part = Part("p1", "SKU", "Name", quantity = 4, minQuantity = 5, "Bin", "2026-10-27")
        assertTrue(part.lowStock)
    }

    @Test
    fun `lowStock is false when quantity is above minQuantity`() {
        val part = Part("p1", "SKU", "Name", quantity = 6, minQuantity = 5, "Bin", "2026-10-27")
        assertFalse(part.lowStock)
    }

    @Test
    fun `validateAdjustment returns error for blank reason`() {
        val part = Part("p1", "SKU", "Name", 10, 5, "Bin", "2026-10-27")
        val result = part.validateAdjustment(-2, "")
        assertTrue(result is AdjustmentResult.Rejected)
        assertEquals(AdjustmentError.REASON_REQUIRED, result?.reason)
    }

    @Test
    fun `validateAdjustment returns error for whitespace reason`() {
        val part = Part("p1", "SKU", "Name", 10, 5, "Bin", "2026-10-27")
        val result = part.validateAdjustment(-2, "   ")
        assertTrue(result is AdjustmentResult.Rejected)
        assertEquals(AdjustmentError.REASON_REQUIRED, result?.reason)
    }

    @Test
    fun `validateAdjustment allows exact to zero quantity`() {
        val part = Part("p1", "SKU", "Name", 10, 5, "Bin", "2026-10-27")
        val result = part.validateAdjustment(-10, "Used all")
        assertEquals(null, result)
    }

    @Test
    fun `validateAdjustment returns error for below zero quantity`() {
        val part = Part("p1", "SKU", "Name", 10, 5, "Bin", "2026-10-27")
        val result = part.validateAdjustment(-11, "Used too many")
        assertTrue(result is AdjustmentResult.Rejected)
        assertEquals(AdjustmentError.INSUFFICIENT_STOCK, result?.reason)
    }
}
