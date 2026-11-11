package dev.aliziwa.jobnotes.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.aliziwa.jobnotes.data.assistant.LastSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    vm: NoteDetailViewModel,
    onBack: () -> Unit,
) {
    val note by vm.note.collectAsStateWithLifecycle()
    val summary by vm.summary.collectAsStateWithLifecycle()
    var body by remember(note?.id) { mutableStateOf(note?.body ?: "") }

    // Summarize as soon as the note is on screen; the technician should not have to ask.
    LaunchedEffect(note?.id) {
        if (note != null) vm.summarize()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(note?.title ?: "Note") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            val n = note
            if (n == null) {
                Text("Note not found. It may have been deleted.", color = MaterialTheme.colorScheme.error)
                return@Column
            }

            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Job note") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row {
                OutlinedButton(onClick = { vm.saveBody(body) }) { Text("Save") }
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = summary !is SummaryUiState.Working,
                    onClick = { vm.summarize() },
                ) { Text("Summarize") }
            }
            Spacer(Modifier.height(16.dp))

            Text("Assistant", style = MaterialTheme.typography.titleMedium)
            when (val s = summary) {
                SummaryUiState.Idle -> Text("Tap Summarize.", style = MaterialTheme.typography.bodyMedium)
                SummaryUiState.Working -> Row {
                    CircularProgressIndicator(Modifier.width(20.dp).height(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Thinking...")
                }
                is SummaryUiState.Done -> {
                    Text(s.summary, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    if (s.parts.isEmpty()) {
                        Text("No parts used.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("Parts used", style = MaterialTheme.typography.titleSmall)
                        s.parts.forEach { p ->
                            Text(p.sku + "  x" + p.qty, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                is SummaryUiState.Failed -> Text(s.message, color = MaterialTheme.colorScheme.error)
            }

            LastSummary.stockReport?.let { report ->
                Spacer(Modifier.height(16.dp))
                Text("Stock reported to dispatch: $report", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
