package nes.app

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import nes.app.components.ErrorScreen
import nes.app.components.LoadingScreen
import nes.app.components.SelectionData
import nes.app.components.SelectionScreen
import nes.app.ui.NesTheme
import nes.app.util.NetworkState
import nes.app.util.mapCollection
import nes.app.util.toSimpleFormat
import nes.networking.phishin.model.Track

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
fun NesApp(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
) {
    val context = LocalContext.current
    var controllerFuture by remember { mutableStateOf<ListenableFuture<MediaController>?>(null) }
    var mediaController by remember { mutableStateOf<MediaController?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
                val cf = MediaController.Builder(context, sessionToken)
                    .buildAsync()
                controllerFuture = cf

                cf.addListener({ mediaController = cf.get() }, MoreExecutors.directExecutor())
            } else if (event == Lifecycle.Event.ON_STOP) {
                controllerFuture?.let { MediaController.releaseFuture(it) }
                mediaController = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }


    val navController = rememberNavController()
    NesNavController(
        mediaController = mediaController,
        navController = navController
    )
}

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowScreen(
    mediaController: MediaController?,
    viewModel: ShowViewModel = hiltViewModel(),
    upClick: () -> Unit
) {
    val show by viewModel.show.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = viewModel.venue) },
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
            when(val s = show) {
                is NetworkState.Error -> ErrorScreen(s.error)
                is NetworkState.Loaded -> {
                    // todo attempt to load media player if null or
                    // move to using compose to load it in a remember
                    val loadedMediaController = checkNotNull(mediaController) { "Should be loaded by now" }
                    val items = s.value.tracks.map {
                        MediaItem.fromUri(it.mp3.toString())
                    }
                    loadedMediaController.addMediaItems(items)
                    loadedMediaController.play()
                }
                NetworkState.Loading -> LoadingScreen()
            }
        }
    }
}

@Composable
fun TrackRow(track: Track, onClick: () -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .height(80.dp)
        .background(Color.Blue)) {
        Box {
            // icon goes here
        }
        Text(text = track.title)
    }
}

@Composable
fun ShowSelectionScreen(
    viewModel: ShowSelectionViewModel = hiltViewModel(),
    navigateUpClick: () -> Unit,
    onShowClicked: (showId: Long, venue: String) -> Unit
) {
    val state by viewModel.shows.collectAsState()
    val selectionData = state.mapCollection {
        SelectionData(
            title = it.venue_name,
            subtitle = it.date.toSimpleFormat()
        ) {
            onShowClicked(it.id, it.venue_name)
        }
    }

    SelectionScreen(
        title = viewModel.showYear,
        state = selectionData,
        upClick = navigateUpClick
    )
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

    SelectionScreen(state = selectionData, upClick = null)
}
