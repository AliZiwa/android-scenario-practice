package dev.aliziwa.dispatch.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.aliziwa.dispatch.data.WorkOrdersRepository
import dev.aliziwa.dispatch.data.remote.FakeDispatchBackend
import dev.aliziwa.dispatch.domain.WorkOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkOrdersListScreen(
    repository: WorkOrdersRepository,
    backend: FakeDispatchBackend,
    onOpen: (String) -> Unit,
) {
    val viewModel: WorkOrdersListViewModel = viewModel(
        factory = viewModelFactory { initializer { WorkOrdersListViewModel(repository) } },
    )
    val state by viewModel.uiState.collectAsState()
    val offline by backend.offline.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dispatch") },
                actions = {
                    Text("Offline", style = MaterialTheme.typography.labelLarge)
                    Switch(
                        checked = offline,
                        onCheckedChange = { backend.offline.value = it },
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    Button(onClick = viewModel::refresh, enabled = !state.isRefreshing) { Text("Refresh") }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.workOrders.isEmpty() && state.isRefreshing) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.workOrders, key = { it.id }) { order ->
                        WorkOrderRow(order = order, onClick = { onOpen(order.id) })
                        HorizontalDivider()
                    }
                }
            }
            state.error?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun WorkOrderRow(order: WorkOrder, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(order.title, style = MaterialTheme.typography.titleMedium)
        Row {
            Text(order.site, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.width(12.dp))
            Text(order.priority.name, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = "${order.status.name.replace('_', ' ')} (v${order.version})",
            style = MaterialTheme.typography.labelSmall,
        )
        if (order.isPending) {
            Spacer(Modifier.height(4.dp))
            Text("Pending sync", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
        }
    }
}
