package nes.app

import androidx.compose.runtime.Composable
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import nes.app.player.NesPlayer
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
            YearSelectionScreen(
                mediaController = mediaController,
                onMiniPlayerClick = { },
                onYearClicked = { navController.navigate(Screen.ShowSelection.createRoute(it)) }
            )
        }
        composable(
            route = Screen.ShowSelection.route,
            arguments = Screen.ShowSelection.navArguments
        ) {
            ShowSelectionScreen(
                mediaController = mediaController,
                navigateUpClick = { navController.navigateUp() },
                onShowClicked = { id, venue -> navController.navigate(Screen.Show.createRoute(id, venue)) },
                onMiniPlayerClick = { navController.navigate(Screen.Player.route) }
            )
        }
        composable(
            route = Screen.Show.route,
            arguments = Screen.Show.navArguments
        ) {
            ShowScreen(
                mediaController = mediaController,
                upClick = { navController.navigateUp() },
                onMiniPlayerClick = { navController.navigate(Screen.Player.route) }
            )
        }
        composable(
            route = Screen.Player.route
        ) {
            NesPlayer(mediaController = mediaController)
        }
    }
}
