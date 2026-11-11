package dev.aliziwa.jobnotes.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.aliziwa.jobnotes.ui.screens.NoteDetailScreen
import dev.aliziwa.jobnotes.ui.screens.NoteDetailViewModel
import dev.aliziwa.jobnotes.ui.screens.NotesListScreen
import dev.aliziwa.jobnotes.ui.screens.NotesListViewModel
import kotlinx.serialization.Serializable

@Serializable
object NotesList

@Serializable
data class NoteDetail(val noteId: String)

@Composable
fun JobNotesNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = NotesList) {
        composable<NotesList> {
            NotesListScreen(
                vm = viewModel(factory = NotesListViewModel.Factory),
                onOpenNote = { navController.navigate(NoteDetail(it)) },
            )
        }
        composable<NoteDetail> {
            NoteDetailScreen(
                vm = viewModel(factory = NoteDetailViewModel.Factory),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
