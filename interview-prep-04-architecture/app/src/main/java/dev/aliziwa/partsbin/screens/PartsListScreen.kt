package dev.aliziwa.partsbin.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.aliziwa.partsbin.Globals

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartsListScreen(onOpenPart: () -> Unit) {
    val vm: PartsListViewModel = viewModel()
    val rows by vm.rows.collectAsState()
    val loading by vm.loading.collectAsState()
    val message by vm.message.collectAsState()
    val query by vm.query.collectAsState()
    var offline by remember { mutableStateOf(Globals.offline) }

    LaunchedEffect(Unit) {
        if (Globals.listDirty) vm.load()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PartsBin") },
                actions = {
                    Text("Offline", style = MaterialTheme.typography.labelMedium)
                    Switch(
                        checked = offline,
                        onCheckedChange = {
                            offline = it
                            Globals.offline = it
                        },
                    )
                    TextButton(onClick = { vm.load() }) { Text("Refresh") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { vm.onQueryChange(it) },
                label = { Text("Search sku or name") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            message?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
            }
            Box(Modifier.fillMaxSize()) {
                if (loading && rows.isEmpty()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                LazyColumn(Modifier.fillMaxSize()) {
                    items(rows, key = { it.id }) { row ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    vm.select(row.id)
                                    onOpenPart()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(row.sku, style = MaterialTheme.typography.titleSmall)
                                Text(row.name, style = MaterialTheme.typography.bodyMedium)
                                Text("Bin " + row.bin, style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = row.quantity.toString() + if (row.lowStock) "  LOW" else "",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (row.lowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
