package dev.aliziwa.partsbin.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartDetailScreen(onBack: () -> Unit) {
    val vm: PartDetailViewModel = viewModel()
    val part by vm.part.collectAsState()
    val loading by vm.loading.collectAsState()
    val busy by vm.busy.collectAsState()
    val message by vm.message.collectAsState()
    var reason by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("1") }

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(part?.sku ?: "Part") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (loading) {
                CircularProgressIndicator()
            }
            part?.let { p ->
                Text(p.name, style = MaterialTheme.typography.titleLarge)
                Text("Bin " + p.binLocation, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "In stock: " + p.quantity + " (min " + p.minQuantity + ")" + if (vm.isLowStock()) "  LOW STOCK" else "",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (vm.isLowStock()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                Text("Updated " + p.updatedAt + ", v" + p.version, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    Button(
                        enabled = !busy,
                        onClick = { vm.adjust(-(amount.toIntOrNull() ?: 0), reason) },
                    ) { Text("Use") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        enabled = !busy,
                        onClick = { vm.adjust(amount.toIntOrNull() ?: 0, reason) },
                    ) { Text("Restock") }
                }
            }
            message?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = { vm.clearMessage() }) { Text("Dismiss") }
            }
        }
    }
}
