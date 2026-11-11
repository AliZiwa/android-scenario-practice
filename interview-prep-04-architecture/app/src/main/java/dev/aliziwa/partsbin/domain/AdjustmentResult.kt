package dev.aliziwa.partsbin.domain

sealed interface AdjustmentResult {
    data class Success(val part: Part) : AdjustmentResult
    data class Rejected(val reason: AdjustmentError) : AdjustmentResult
    data class ServerRejected(val message: String) : AdjustmentResult
    data object NotFound : AdjustmentResult
    data object Offline : AdjustmentResult
}

enum class AdjustmentError {
    REASON_REQUIRED,
    INSUFFICIENT_STOCK
}

fun Part.validateAdjustment(delta: Int, reason: String): AdjustmentResult.Rejected? {
    if (reason.isBlank()) return AdjustmentResult.Rejected(AdjustmentError.REASON_REQUIRED)
    if (quantity + delta < 0) return AdjustmentResult.Rejected(AdjustmentError.INSUFFICIENT_STOCK)
    return null
}
