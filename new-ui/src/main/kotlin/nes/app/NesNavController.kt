package nes.app

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.media3.common.Player
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import nes.app.player.FullPlayer
import nes.app.show.ShowScreen
import nes.app.show.ShowSelectionScreen
import nes.app.year.YearSelectionScreen

@ExperimentalMaterial3Api
@Composable
fun NesNavController(
    musicPlayer: Player?,
    navController: NavHostController
) {
    NavHost(navController = navController, startDestination = Screen.YearSelection.route) {
        composable(route = Screen.YearSelection.route) {
            YearSelectionScreen(
                musicPlayer = musicPlayer,
                onMiniPlayerClick = { },
                onYearClicked = { navController.navigate(Screen.ShowSelection.createRoute(it)) }
            )
        }
        composable(
            route = Screen.ShowSelection.route,
            arguments = Screen.ShowSelection.navArguments
        ) {
            ShowSelectionScreen(
                musicPlayer = musicPlayer,
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
                musicPlayer = musicPlayer,
                upClick = { navController.navigateUp() },
                onMiniPlayerClick = { navController.navigate(Screen.Player.route) }
            )
        }
        composable(
            route = Screen.Player.route
        ) {
            FullPlayer(
                player = musicPlayer,
                upClick = { navController.navigateUp() },
            )
        }
    }
}
