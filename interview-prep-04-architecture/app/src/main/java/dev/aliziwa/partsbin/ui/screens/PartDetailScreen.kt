package dev.aliziwa.partsbin.ui.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartDetailScreen(
    vm: PartDetailViewModel,
    onBack: () -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("1") }
    val busy by vm.busy.collectAsStateWithLifecycle()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val part = (uiState as? PartsDetailState.Loaded)?.rowItem

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(part?.sku ?: "Part") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { padding ->
        Column(Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)) {
            when (val s = uiState) {
                PartsDetailState.Loading -> CircularProgressIndicator()
                is PartsDetailState.Loaded -> {
                    val p = s.rowItem
                    Text(p.name, style = MaterialTheme.typography.titleLarge)
                    Text("Bin " + p.bin, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "In stock: " + p.quantity + " (min " + p.minQuantity + ")" + if (p.lowStock) "  LOW STOCK" else "",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (p.lowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "Updated " + p.updatedAt + ", v" + p.version,
                        style = MaterialTheme.typography.labelSmall
                    )
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

                    message?.let { msg ->
                        InformationMessage(message = msg, color = MaterialTheme.colorScheme.primary) {
                            vm.dismissMessage()
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun InformationMessage(message: String, color: Color, onDismiss: () -> Unit) {
    Spacer(Modifier.height(8.dp))
    Text(message, color = color)
    TextButton(onClick = onDismiss) { Text("Dismiss") }
}
