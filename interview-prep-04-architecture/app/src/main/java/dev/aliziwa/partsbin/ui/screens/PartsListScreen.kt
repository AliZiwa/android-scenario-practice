package dev.aliziwa.partsbin.ui.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.aliziwa.partsbin.AppContainer
import dev.aliziwa.partsbin.data.PartsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartsListScreen(
    vm: PartsListViewModel,
    onOpenPart: (String) -> Unit
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val query by remember { mutableStateOf("") }
    var offline by remember { AppContainer.offline }


    LaunchedEffect(Unit) {
        vm.load()
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
                        },
                    )
                    TextButton(onClick = { vm.load() }) { Text("Refresh") }
                },
            )
        },
    ) { padding ->
        Column(Modifier
            .fillMaxSize()
            .padding(padding)) {
            val loading = uiState is PartsListState.Loading
            val rowItems = when (uiState) {
                is PartsListState.Loaded -> (uiState as PartsListState.Loaded).rows
                else -> emptyList()
            }
            val message = when (uiState) {
                is PartsListState.Error -> (uiState as PartsListState.Error).message
                else -> null
            }

            OutlinedTextField(
                value = query,
                onValueChange = { vm.onQueryChange(it) },
                label = { Text("Search sku or name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            message?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            Box(Modifier.fillMaxSize()) {
                if (loading) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                LazyColumn(Modifier.fillMaxSize()) {
                    items(rowItems, key = { it.id }) { row ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOpenPart(row.id)
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
