package dev.aliziwa.fieldnotes.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import dev.aliziwa.fieldnotes.data.NotesRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: Long,
    repository: NotesRepository,
    onBack: () -> Unit,
) {
    val viewModel: NoteDetailViewModel = viewModel(
        key = "note-$noteId",
        factory = viewModelFactory { initializer { NoteDetailViewModel(noteId, repository) } },
    )
    val noteUiState by viewModel.noteUiState.collectAsState()
    val note = (noteUiState as? NoteDetailUiState.Loaded)?.note

    LaunchedEffect(noteUiState) {
        if(noteUiState is NoteDetailUiState.Gone) {
             onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(note?.title ?: "Loading") },
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
            Text(note?.body ?: "", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))
            Row {
                Button(onClick = { viewModel.appendBody("Follow-up logged.") }) { Text("Append") }
                Spacer(Modifier.width(12.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.delete()
                    },
                ) { Text("Delete") }
            }
        }
    }
}
