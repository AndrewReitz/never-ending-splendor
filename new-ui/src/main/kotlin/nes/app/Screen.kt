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
    )  {
        fun createRoute(year: String) = "shows/$year"
    }

    data object Show : Screen(
        route = "show/{id}",
        navArguments = listOf(navArgument("id") { type = NavType.LongType })
    ) {
        fun createRoute(showId: Long) = "show/$showId"
    }
}
