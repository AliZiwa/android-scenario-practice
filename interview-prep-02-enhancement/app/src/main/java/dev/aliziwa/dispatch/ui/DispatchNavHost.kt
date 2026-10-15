package dev.aliziwa.dispatch.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.aliziwa.dispatch.di.AppContainer
import dev.aliziwa.dispatch.ui.detail.WorkOrderDetailScreen
import dev.aliziwa.dispatch.ui.list.WorkOrdersListScreen

/**
 * Two screens, no navigation library, same as assignment 01. Routing is not the exercise.
 */
@Composable
fun DispatchNavHost(container: AppContainer) {
    var selectedId: String? by rememberSaveable { mutableStateOf<String?>(null) }

    val id = selectedId
    if (id == null) {
        WorkOrdersListScreen(
            repository = container.repository,
            backend = container.backend,
            onOpen = { selectedId = it },
        )
    } else {
        WorkOrderDetailScreen(
            workOrderId = id,
            repository = container.repository,
            onBack = { selectedId = null },
        )
    }
}
