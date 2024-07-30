package nes.app

import androidx.compose.runtime.Composable
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import nes.app.show.ShowScreen
import nes.app.show.ShowSelectionScreen
import nes.app.year.YearSelectionScreen

@Composable
fun NesNavController(
    mediaController: MediaController?,
    navController: NavHostController
) {
    NavHost(navController = navController, startDestination = Screen.YearSelection.route) {
        composable(route = Screen.YearSelection.route) {
            YearSelectionScreen {
                navController.navigate(Screen.ShowSelection.createRoute(it))
            }
        }
        composable(
            route = Screen.ShowSelection.route,
            arguments = Screen.ShowSelection.navArguments
        ) {
            ShowSelectionScreen(navigateUpClick = { navController.navigateUp() }) { id, venue ->
                navController.navigate(Screen.Show.createRoute(id, venue))
            }
        }
        composable(
            route = Screen.Show.route,
            arguments = Screen.Show.navArguments
        ) {
            ShowScreen(mediaController = mediaController) {
                navController.navigateUp()
            }
        }
    }
}
