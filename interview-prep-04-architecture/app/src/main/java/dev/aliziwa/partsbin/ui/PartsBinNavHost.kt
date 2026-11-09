package dev.aliziwa.partsbin.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.aliziwa.partsbin.AppContainer
import dev.aliziwa.partsbin.ui.screens.PartDetailScreen
import dev.aliziwa.partsbin.ui.screens.PartDetailViewModel
import dev.aliziwa.partsbin.ui.screens.PartsListScreen
import dev.aliziwa.partsbin.ui.screens.PartsListViewModel
import kotlinx.serialization.Serializable

@Serializable
object PartsList

@Serializable
data class PartsDetail(
    val selectedPartId: String,
)

@Composable
fun PartsBinNavHost(container: AppContainer) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = PartsList) {
        composable<PartsList> {
            PartsListScreen(
                vm = viewModel(factory = PartsListViewModel.Factory),
                onOpenPart = { navController.navigate(PartsDetail(it)) }
            )
        }
        composable<PartsDetail> { backStackEntry ->
            val partsDetail = backStackEntry.toRoute<PartsDetail>()

            PartDetailScreen(
                vm = viewModel(factory = PartDetailViewModel.Factory),
                selectedPartId = partsDetail.selectedPartId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}