package dev.aliziwa.partsbin.domain

sealed interface RefreshResult {
    data object Success : RefreshResult
    data object Offline : RefreshResult
    data class Error(val message: String) : RefreshResult
}
