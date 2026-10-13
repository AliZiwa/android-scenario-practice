package dev.aliziwa.fieldnotes.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.aliziwa.fieldnotes.di.AppContainer
import dev.aliziwa.fieldnotes.ui.detail.NoteDetailScreen
import dev.aliziwa.fieldnotes.ui.list.NotesListScreen

/**
 * Two screens, no navigation library: the exercise is about state and lifecycle, not routing.
 */
@Composable
fun FieldNotesNavHost(container: AppContainer) {
    var selectedNoteId: Long? by rememberSaveable { mutableStateOf<Long?>(null) }

    val id = selectedNoteId
    if (id == null) {
        NotesListScreen(
            repository = container.repository,
            onOpenNote = { selectedNoteId = it },
        )
    } else {
        NoteDetailScreen(
            noteId = id,
            repository = container.repository,
            onBack = { selectedNoteId = null },
        )
    }
}
