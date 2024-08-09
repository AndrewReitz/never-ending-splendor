@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package nes.app.ui.player

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import nes.app.R
import nes.app.ui.components.CastButton
import nes.app.ui.components.LoadingScreen
import nes.app.ui.components.TopAppBarText
import nes.app.util.artworkUri
import nes.app.util.mediaMetaData
import nes.app.util.title
import kotlin.math.max

@Composable
fun FullPlayer(
    viewModel: PlayerViewModel = hiltViewModel(),
    navigateToShow: (showId: Long, venueName: String) -> Unit,
    upClick: () -> Unit,
) {
    val playerState by viewModel.playerState.collectAsState()

    FullPlayer(
        playerState = playerState,
        title = checkNotNull(viewModel.title),
        navigateToShow = navigateToShow,
        upClick = upClick,
        seekTo = viewModel::seekTo,
        seekToPreviousMediaItem = viewModel::seekToPreviousMediaItem,
        seekToNextMediaItem = viewModel::seekToNextMediaItem,
        onPause = viewModel::pause,
        onPlay = viewModel::play,
        actions = { CastButton() }
    )
}

@Composable
fun FullPlayer(
    playerState: PlayerState,
    title: String,
    navigateToShow: (showId: Long, venueName: String) -> Unit,
    upClick: () -> Unit,
    seekTo: (Long) -> Unit,
    seekToPreviousMediaItem: () -> Unit,
    seekToNextMediaItem: () -> Unit,
    onPause: () -> Unit,
    onPlay: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { TopAppBarText(title) },
                navigationIcon = {
                    IconButton(onClick = upClick) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.navigate_back)
                        )
                    }
                },
                actions = actions
            )
        }
    ) { innerPadding ->
        when (val ps = playerState) {
            is PlayerState.NoMedia -> LoadingScreen()
            is PlayerState.MediaLoaded -> {
                val currentMediaItem = ps.mediaItem
                val playing = ps.isPlaying
                val duration = ps.duration

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                val (showId, venueName) = currentMediaItem.mediaMetaData
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
                        text = currentMediaItem.title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .basicMarquee(Int.MAX_VALUE)
                            .padding(8.dp)
                    )

                    var sliderValue by remember {
                        mutableFloatStateOf(ps.currentPosition.toFloat())
                    }

                    LaunchedEffect(ps.currentPosition) {
                        sliderValue = ps.currentPosition.toFloat()
                    }

                    Slider(
                        value = sliderValue,
                        onValueChange = {
                            seekTo(it.toLong())
                        },
                        valueRange = 0f .. max(duration.toFloat(), 0f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = seekToPreviousMediaItem) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "previous song"
                            )
                        }
                        IconButton(
                            onClick = {
                                if (playing) onPause() else onPlay()
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
                        IconButton(onClick = seekToNextMediaItem) {
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
}
