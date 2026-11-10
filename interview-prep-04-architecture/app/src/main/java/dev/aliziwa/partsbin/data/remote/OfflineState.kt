package dev.aliziwa.partsbin.data.remote

import kotlinx.coroutines.flow.MutableStateFlow

class OfflineState(
    val isOffline: MutableStateFlow<Boolean> = MutableStateFlow(false)
)
