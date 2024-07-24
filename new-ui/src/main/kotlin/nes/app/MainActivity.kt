package nes.app

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import nes.app.ui.NesTheme
import nes.app.ui.Rainbow
import nes.networking.phishin.model.YearData

@AndroidEntryPoint
class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            NesTheme {
                NesApp()
            }
        }
    }
}

@Composable
fun NesApp() {
    val navController = rememberNavController()
    NesNavController(navController = navController)
}

@Composable
fun NesNavController(
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
            ShowSelectionScreen(navigateUpClick = { navController.navigateUp() }) {
                navController.navigate(Screen.Show.createRoute(it))
            }
        }
        composable(
            route = Screen.Show.route,
            arguments = Screen.Show.navArguments
        ) {
            ShowScreen()
        }
    }
}

@Composable
fun ShowScreen(
    viewModel: ShowViewModel = hiltViewModel()
) {
    val state by viewModel.show.collectAsState()
    val temp = state.map {
        it.tracks
    }.mapCollection {
        SelectionData(
            title = it.title,
            subtitle = "",
            onClick = {}
        )
    }

    SelectionScreen(state = temp) {

    }
}

@Composable
fun ShowSelectionScreen(
    viewModel: ShowSelectionViewModel = hiltViewModel(),
    navigateUpClick: () -> Unit,
    onShowClicked: (showId: Long) -> Unit
) {
    val state by viewModel.shows.collectAsState()
    val selectionData = state.mapCollection {
        SelectionData(
            title = it.venue_name,
            subtitle = it.date.toSimpleFormat()
        ) {
            onShowClicked(it.id)
        }
    }

    SelectionScreen(selectionData, navigateUpClick)
}


@Composable
fun YearSelectionScreen(
    viewModel: YearSelectionViewModel = hiltViewModel(),
    onYearClicked: (year: String) -> Unit
) {
    val state by viewModel.years.collectAsState()
    val selectionData = state.mapCollection {
        SelectionData(title = it.date, subtitle = "${it.show_count} shows") {
            onYearClicked(it.date)
        }
    }

    SelectionScreen(selectionData, null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionScreen(state: NetworkState<List<SelectionData>, String>, upClick: (() -> Unit)?) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "App Name Todo") },
                navigationIcon = {
                    upClick?.let {
                        IconButton(onClick = upClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Navigate Back"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when(state) {
                is NetworkState.Error -> ErrorScreen(state.error)
                is NetworkState.Loaded -> SelectionList(state.value)
                NetworkState.Loading -> LoadingScreen()
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

data class SelectionData(
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)

@Composable
fun SelectionList(
    data: List<SelectionData>,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.then(Modifier.fillMaxSize())) {
        itemsIndexed(data) { i, (title, subtitle, onClick) ->
            SelectionRow(
                title = title,
                subtitle = subtitle,
                boxColor = Rainbow[i % Rainbow.size],
                onClick = onClick
            )
        }
    }
}

@Composable
fun SelectionRow(
    title: String,
    subtitle: String,
    boxColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .fillMaxWidth()
            .clickable {
                onClick()
            }
    ) {
        Box(modifier = Modifier
            .width(80.dp)
            .height(96.dp)
            .background(boxColor))

        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

