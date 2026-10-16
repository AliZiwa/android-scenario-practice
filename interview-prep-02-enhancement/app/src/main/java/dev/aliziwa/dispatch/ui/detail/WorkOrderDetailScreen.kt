package dev.aliziwa.dispatch.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.aliziwa.dispatch.data.WorkOrdersRepository
import dev.aliziwa.dispatch.domain.Status

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkOrderDetailScreen(
    workOrderId: String,
    repository: WorkOrdersRepository,
    onBack: () -> Unit,
) {
    val viewModel: WorkOrderDetailViewModel = viewModel(
        key = "work-order-$workOrderId",
        factory = viewModelFactory {
            initializer {
                WorkOrderDetailViewModel(
                    workOrderId,
                    repository
                )
            }
        },
    )
    val state by viewModel.uiState.collectAsState()
    val loadedState = state as? WorkOrderDetailUiState.Loaded
    val order = loadedState?.workOrder

    LaunchedEffect(state) {
        if (state is WorkOrderDetailUiState.Gone) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(order?.title ?: "Loading") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            if (order != null) {
                Text("Site: ${order.site}", style = MaterialTheme.typography.bodyLarge)
                Text("Priority: ${order.priority.name}", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Status: ${order.status.name.replace('_', ' ')}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(8.dp))
                Text("Server version ${order.version}", style = MaterialTheme.typography.labelSmall)

                if (order.isPending) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Pending sync",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (loadedState?.message != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        loadedState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(Modifier.height(24.dp))
                Row(
                    Modifier.fillMaxWidth()
                ) {
                    val currentStatus = order.status
                    Button(
                        enabled = currentStatus == Status.OPEN,
                        onClick = { viewModel.updateStatus() }
                    ) {
                        Text("Start")
                    }
                    Spacer(Modifier.width(24.dp))
                    Button(
                        enabled = currentStatus == Status.IN_PROGRESS,
                        onClick = { viewModel.updateStatus() }
                    ) {
                        Text("Complete")
                    }
                }
            }
        }
    }
}
