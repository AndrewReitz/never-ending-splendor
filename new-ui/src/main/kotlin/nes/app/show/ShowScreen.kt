@file:OptIn(ExperimentalMaterial3Api::class)

package nes.app.show

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import nes.app.R
import nes.app.components.CastButton
import nes.app.components.ErrorScreen
import nes.app.components.LoadingScreen
import nes.app.player.MiniPlayer
import nes.app.ui.NesTheme
import nes.app.ui.Rainbow
import nes.app.util.LCE
import nes.app.util.toAlbumFormat
import nes.app.util.yearString
import nes.networking.phishin.model.Show

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowScreen(
    musicPlayer: Player?,
    viewModel: ShowViewModel = hiltViewModel(),
    upClick: () -> Unit,
    onMiniPlayerClick: () -> Unit
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
                },
                actions = {
                    CastButton()
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
                is LCE.Error -> ErrorScreen(state.error)
                is LCE.Loaded -> {
                    if (musicPlayer != null) {
                        ShowListWithPlayer(
                            show = state.value,
                            musicPlayer = musicPlayer,
                            onMiniPlayerClick = onMiniPlayerClick
                        )
                    } else {
                        LoadingScreen()
                    }
                }
                LCE.Loading -> LoadingScreen()
            }
        }
    }
}

@Composable
fun ShowListWithPlayer(
    show: Show,
    musicPlayer: Player,
    onMiniPlayerClick: () -> Unit,
) {
    LaunchedEffect(show) {
        val items = show.tracks.map {
            MediaItem.Builder()
                .setUri(it.mp3)
                .setMediaId(it.mp3)
                .setMimeType(MimeTypes.AUDIO_MPEG)
                .setTag(show)
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

        musicPlayer.addMediaItems(items)
        musicPlayer.prepare()
    }

    var currentlyPlayingMediaId by remember {
        mutableStateOf(musicPlayer.currentMediaItem?.mediaId)
    }
    var playing by remember { mutableStateOf(musicPlayer.isPlaying) }
    val playerListener by remember {
        mutableStateOf(
            object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    currentlyPlayingMediaId = mediaItem?.mediaId
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    playing = isPlaying
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            musicPlayer.removeListener(playerListener)
        }
    }

    LaunchedEffect(Unit) {
        musicPlayer.addListener(playerListener)
    }

    var firstLoad by remember { mutableStateOf(true) }

    Column {
        LazyColumn(modifier = Modifier
            .fillMaxSize()
            .weight(1f)) {
            itemsIndexed(show.tracks) { i, track ->
                val isPlaying = track.mp3 == currentlyPlayingMediaId && playing

                TrackRow(
                    boxColor = Rainbow[i % Rainbow.size],
                    trackTitle = track.title,
                    duration = track.formatedDuration,
                    playing = isPlaying
                ) {
                    if (!isPlaying) {
                        if (firstLoad) {
                            firstLoad = false
                            for (ic in 0 until musicPlayer.mediaItemCount) {
                                val m = musicPlayer.getMediaItemAt(ic)
                                if (m.mediaId == show.tracks.first().mp3) {
                                    musicPlayer.removeMediaItems(0, ic)
                                    break
                                }
                            }
                        }

                        currentlyPlayingMediaId = track.mp3
                        musicPlayer.seekTo(i, 0)
                        musicPlayer.play()
                    } else {
                        musicPlayer.pause()
                    }
                }
            }
        }
        MiniPlayer(
            musicPlayer = musicPlayer,
            onClick = onMiniPlayerClick
        )
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
