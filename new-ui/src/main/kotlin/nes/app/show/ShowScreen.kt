@file:OptIn(ExperimentalMaterial3Api::class)

package nes.app.show

import android.text.format.DateUtils
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import nes.app.R
import nes.app.components.ErrorScreen
import nes.app.components.LoadingScreen
import nes.app.ui.NesTheme
import nes.app.ui.Rainbow
import nes.app.util.NetworkState
import nes.app.util.toAlbumFormat
import nes.app.util.yearString
import nes.networking.phishin.model.Show

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowScreen(
    mediaController: MediaController?,
    viewModel: ShowViewModel = hiltViewModel(),
    upClick: () -> Unit
) {
    val showState by viewModel.show.collectAsState()
    val appBarTitle by viewModel.appBarTitle.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = appBarTitle) },
                navigationIcon = {
                    IconButton(onClick = upClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate Back"
                        )
                    }
                }
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when(val state = showState) {
                is NetworkState.Error -> ErrorScreen(state.error)
                is NetworkState.Loaded -> ShowListWithPlayer(
                    show = state.value,
                    mediaController = checkNotNull(mediaController) { "Should be loaded by now" }
                )
                NetworkState.Loading -> LoadingScreen()
            }
        }
    }
}

@Composable
fun ShowListWithPlayer(show: Show, mediaController: MediaController) {
    val items = show.tracks.map {
        MediaItem.Builder()
            .setUri(it.mp3.toString())
            .setMediaId(it.mp3.toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setArtist("Phish")
                    .setAlbumArtist("Phish")
                    .setAlbumTitle("${show.date.toAlbumFormat()} ${show.venue.location}")
                    .setTitle(it.title)
                    .setRecordingYear(show.date.yearString.toInt())
                    .build()
            )
            .build()
    }
    mediaController.addMediaItems(items)

    var currentlyPlayingMediaId by remember { mutableStateOf<String?>(null) }
    var currentlyPlayingTrackName by remember { mutableStateOf("--") }
    var playing by remember { mutableStateOf(mediaController.isPlaying) }
    val playerListener by remember {
        mutableStateOf(
            object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    currentlyPlayingMediaId = mediaItem?.mediaId
                    currentlyPlayingTrackName = mediaItem?.mediaMetadata?.title?.toString() ?: "--"
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    playing = isPlaying
                }

                override fun onPlayerError(error: PlaybackException) {
                    // TODO Log error
                }
            }
        )
    }

    DisposableEffect(show) {
        onDispose {
            mediaController.removeListener(playerListener)
        }
    }

    LaunchedEffect(true) {
        mediaController.addListener(playerListener)
    }

    Column {
        LazyColumn(modifier = Modifier
            .fillMaxSize()
            .weight(1f)) {
            itemsIndexed(show.tracks) { i, track ->
                val isPlaying = track.mp3.toString() == currentlyPlayingMediaId && playing

                TrackRow(
                    boxColor = Rainbow[i % Rainbow.size],
                    trackTitle = track.title,
                    duration = track.formatedDuration,
                    playing = isPlaying
                ) {
                    if (!isPlaying) {

                        // check if first load and if it is clear out queue up
                        // to first track

                        currentlyPlayingMediaId = track.mp3.toString()
                        currentlyPlayingTrackName = track.title
                        mediaController.seekTo(i, 0)
                        mediaController.play()
                    } else {
                        mediaController.pause()
                    }
                }
            }
        }

        val scope = rememberCoroutineScope()
        var duration by remember(currentlyPlayingMediaId) { mutableStateOf( "--:--") }
        LaunchedEffect(currentlyPlayingMediaId) {
            scope.launch {
                while(true) {
                    delay(1000)
                    duration = DateUtils.formatElapsedTime(mediaController.currentPosition / 1000)
                }
            }
        }

        MiniPlayer(
            trackTitle = currentlyPlayingTrackName,
            duration = duration,
            isPlaying = playing
        ) {
            if (playing) {
                mediaController.pause()
            } else {
                mediaController.play()
            }
        }
    }
}

@Composable
fun TrackRow(
    boxColor: Color,
    trackTitle: String,
    duration: String,
    playing: Boolean,
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
        IconButton(modifier = Modifier
            .width(80.dp)
            .height(80.dp)
            .background(boxColor),
            onClick = {
                onClick()
            }
        ) {
            val (imageVector, contentDescription) = if (playing) {
                Icons.Default.Pause to stringResource(R.string.pause)
            } else {
                Icons.Default.PlayArrow to stringResource(R.string.pause)
            }

            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription
            )
        }

        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = trackTitle,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = duration,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TrackRowPreview() {
    NesTheme {
        TrackRow(
            boxColor = Rainbow[0],
            trackTitle = "The Lizzards",
            duration = "10:00",
            playing = false
        ) {

        }
    }
}

@Composable
fun MiniPlayer(
    trackTitle: String,
    duration: String,
    isPlaying: Boolean,
    playPauseClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color.Cyan),
    ) {
        Box(modifier = Modifier
            .size(56.dp)
            .background(Color.Magenta)
        ) {

        }

        Column(
            modifier = Modifier
                .padding(8.dp)
                .weight(1f)
        ) {
            Text(
                text = trackTitle,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = duration,
                style = MaterialTheme.typography.bodySmall
            )
        }

        IconButton(
            onClick = playPauseClick
        ) {
            val (imageVector, contentDescription) = if (isPlaying) {
                Icons.Default.Pause to stringResource(R.string.pause)
            } else {
                Icons.Default.PlayArrow to stringResource(R.string.pause)
            }

            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MiniPlayerPreview() {
    NesTheme {
        MiniPlayer(
            trackTitle = "Ghost",
            duration = "40:00",
            isPlaying = true,
            playPauseClick = {}
        )
    }
}
