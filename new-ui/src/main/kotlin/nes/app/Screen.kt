package nes.app

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class Screen(
    val route: String,
    val navArguments: List<NamedNavArgument> = emptyList()
) {
    data object YearSelection : Screen("yearSelection")

    data object ShowSelection : Screen(
        route = "shows/{year}",
        navArguments = listOf(navArgument("year") { type = NavType.StringType })
    ) {
        fun createRoute(year: String) = "shows/$year"
    }

    data object Show : Screen(
        route = "show/{id}/{venue}",
        navArguments = listOf(
            navArgument("id") { type = NavType.LongType },
            navArgument("venue") { type = NavType.StringType }
        )
    ) {
        fun createRoute(showId: Long, venue: String) = "show/$showId/$venue"
    }

    data object Player : Screen("player")
}
