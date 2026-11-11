package dev.aliziwa.partsbin.domain

import kotlinx.coroutines.flow.MutableStateFlow

class OfflineState(
    val isOffline: MutableStateFlow<Boolean> = MutableStateFlow(false)
)
