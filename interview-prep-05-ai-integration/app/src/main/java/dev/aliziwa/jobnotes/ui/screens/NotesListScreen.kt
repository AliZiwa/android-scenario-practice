package dev.aliziwa.jobnotes.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    vm: NotesListViewModel,
    onOpenNote: (String) -> Unit,
) {
    val notes by vm.notes.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("JobNotes") }) },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(notes, key = { it.id }) { note ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpenNote(note.id) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(note.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        note.body.ifBlank { "(empty)" },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(note.updatedAt, style = MaterialTheme.typography.labelSmall)
                }
                HorizontalDivider()
            }
        }
    }
}
