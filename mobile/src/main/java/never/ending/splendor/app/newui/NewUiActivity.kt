package never.ending.splendor.app.newui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nes.networking.phishin.model.Show
import never.ending.splendor.app.newui.theme.TestComposeTheme
import nes.networking.phishin.model.YearData
import never.ending.splendor.app.newui.theme.Rainbow
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.android.closestDI
import org.kodein.di.compose.rememberViewModel
import kotlin.reflect.typeOf

inline fun <reified T : Any> serializableType(
    isNullableAllowed: Boolean = false,
    json: Json = Json,
) = object : NavType<T>(isNullableAllowed = isNullableAllowed) {
    override fun get(bundle: Bundle, key: String) =
        bundle.getString(key)?.let<String, T>(json::decodeFromString)

    override fun parseValue(value: String): T = json.decodeFromString(value)

    override fun serializeAsValue(value: T): String = json.encodeToString(value)

    override fun put(bundle: Bundle, key: String, value: T) {
        bundle.putString(key, json.encodeToString(value))
    }
}

@Serializable
sealed class Destination {
    @Serializable data object LoadingScreen: Destination()
    @Serializable data class YearSelectionScreen(val years: List<YearData>) : Destination() {
        companion object {
            val typeMap = mapOf(typeOf<List<YearData>>() to serializableType<List<YearData>>())

            fun from(savedStateHandle: SavedStateHandle) =
                savedStateHandle.toRoute<YearSelectionScreen>(typeMap)
        }
    }
    @Serializable data class ShowSelectionScreen(val shows:List<Show>) : Destination()
    @Serializable data class ErrorScreen(val message: String): Destination()
}

class NewUiActivity : ComponentActivity(), DIAware {

    override val di: DI by closestDI()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TestComposeTheme {
                val viewModel: NewUiViewModel by rememberViewModel()
                val appState by viewModel.appState.collectAsState()

                val navController = rememberNavController()

                App(
                    appState = appState,
                    navController = navController,
                    onYearSelected = { year ->
                        navController.navigate(route = Destination.LoadingScreen)
                        viewModel.loadShows(year)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    appState: AppState,
    navController: NavHostController,
    onYearSelected: (year: String) -> Unit
) {
    NavHost(navController = navController, startDestination = Destination.LoadingScreen) {
        composable<Destination.LoadingScreen> { LoadingScreen() }
        composable<Destination.YearSelectionScreen>(typeMap = Destination.YearSelectionScreen.typeMap) { backStackEntry ->
            val years = backStackEntry.toRoute<Destination.YearSelectionScreen>()
            YearsScreen(years = years.years, onClick = onYearSelected)
        }
        composable<Destination.ShowSelectionScreen> {

        }
        composable<Destination.ErrorScreen> {
            ErrorScreen(message = "Oh No! TODO")
        }
    }

    TestComposeTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = { TopAppBar(title = { Text(text = "App Name Todo") })}
        ) { innerPadding ->
            Box(modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()) {

                when (appState) {
                    AppState.Loading -> navController.navigate(Destination.LoadingScreen)
                    is AppState.Years -> navController.navigate(Destination.YearSelectionScreen(appState.data))
                    is AppState.Error -> ErrorScreen(
                        message = appState.message
                    )
                    is AppState.Shows -> navController.navigate(Destination.ShowSelectionScreen(appState.data))
                }
            }
        }
    }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.then(Modifier.fillMaxSize())) {
        CircularProgressIndicator(
            modifier = Modifier
                .width(64.dp)
                .align(Alignment.Center),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
fun ErrorScreen(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.then(Modifier.fillMaxSize())) {
        Text(text = message)
    }
}

@Composable
fun YearsScreen(
    years: List<YearData>,
    modifier: Modifier = Modifier,
    onClick: (year: String) -> Unit
) {
    LazyColumn(modifier = modifier.then(Modifier.fillMaxSize())) {
        itemsIndexed(years) { i, year ->
            MessageRow(year, Rainbow[i % Rainbow.size], onClick)
        }
    }
}

@Composable
fun MessageRow(
    yearData: YearData,
    boxColor: Color,
    onClick: (year: String) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .fillMaxWidth()
            .clickable {
                onClick(yearData.date)
            }
    ) {
        Box(modifier = Modifier
            .size(72.dp)
            .background(boxColor))

        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = yearData.date,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "${yearData.show_count} shows",
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoadingPreview() {
    val navController = rememberNavController()
    TestComposeTheme {
        App(AppState.Loading, navController = navController, onYearSelected = { })
    }
}
