package nes.app.player

import androidx.annotation.OptIn
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nes.app.R
import nes.app.components.CastButton
import nes.app.components.LoadingScreen
import nes.app.components.TopAppBarText
import nes.app.ui.NesTheme
import nes.app.util.artworkUri
import nes.app.util.mediaMetaData
import nes.app.util.stub
import nes.app.util.title
import javax.inject.Inject
import kotlin.math.max

@HiltViewModel
class FullPlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
): ViewModel() {
    val title: String = checkNotNull(savedStateHandle["title"])
}

@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Composable
fun FullPlayer(
    viewModel: FullPlayerViewModel = hiltViewModel(),
    player: Player?,
    navigateToShow: (showId: Long, venueName: String) -> Unit,
    upClick: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { TopAppBarText(viewModel.title) },
                navigationIcon = {
                    IconButton(onClick = upClick) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.navigate_back)
                        )
                    }
                },
                actions = { CastButton() }
            )
        }
    ) { innerPadding ->

        var currentMediaItem: MediaItem? by remember { mutableStateOf(player?.currentMediaItem) }

        if (player == null || currentMediaItem == null) {
            LoadingScreen()
        } else {
            val mediaItem = checkNotNull(currentMediaItem)
            var playing by remember { mutableStateOf(player.isPlaying) }
            var duration by remember { mutableLongStateOf(player.duration) }

            val playerListener by remember {
                mutableStateOf(
                    object : Player.Listener {
                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            currentMediaItem = mediaItem
                            duration = player.duration
                        }

                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            playing = isPlaying
                            currentMediaItem = player.currentMediaItem
                        }
                    }
                )
            }

            DisposableEffect(Unit) {
                onDispose {
                    player.removeListener(playerListener)
                }
            }

            LaunchedEffect(Unit) {
                player.addListener(playerListener)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            val (showId, venueName) = mediaItem.mediaMetaData
                            navigateToShow(showId, venueName)
                        },
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Text(text = "Go to show")
                    }
                }

                AsyncImage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                    ,
                    contentScale = ContentScale.Fit,
                    model = currentMediaItem.artworkUri,
                    contentDescription = null,
                )

                Text(
                    text = mediaItem.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.basicMarquee(Int.MAX_VALUE)
                        .padding(8.dp)
                )

                val scope = rememberCoroutineScope()
                var sliderPosition by remember { mutableFloatStateOf(player.currentPosition.toFloat()) }
                LaunchedEffect(mediaItem) {
                    scope.launch {
                        while (true) {
                            delay(1000)
                            sliderPosition = player.currentPosition.toFloat()
                        }
                    }
                }

                Slider(
                    value = sliderPosition,
                    onValueChange = {
                        sliderPosition = it
                        player.seekTo(it.toLong())
                    },
                    valueRange = 0f .. max(duration.toFloat(), 1f),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { player.seekToPreviousMediaItem() }) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "previous song"
                        )
                    }
                    IconButton(
                        onClick = {
                            if (playing) {
                                player.pause()
                            } else {
                                player.play()
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors().copy(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = if (!playing) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "play"
                        )
                    }
                    IconButton(onClick = { player.seekToNextMediaItem() }) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "skip to next song"
                        )
                    }
                }

                Spacer(modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp))
            }
        }
    }
}

@ExperimentalMaterial3Api
@Preview(showBackground = true)
@Composable
fun NesPlayerPreview() {
    NesTheme {
        FullPlayer(
            player = object : Player by stub() {
                override fun getCurrentMediaItem() = MediaItem.Builder()
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle("Punch you in the eye")
                            .build()
                    )
                    .build() 
            },
            navigateToShow = { id, venueName ->  }, 
            upClick = { }
        )
    }
}