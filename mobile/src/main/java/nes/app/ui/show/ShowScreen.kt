package nes.app.ui.show

import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import nes.app.ui.components.LoadingScreen
import nes.app.ui.components.NesScaffold
import nes.app.ui.player.MiniPlayer
import nes.app.ui.player.PlayerViewModel
import nes.app.ui.NesTheme
import nes.app.ui.Rainbow
import nes.app.util.stub
import nes.app.util.toAlbumFormat
import nes.app.util.toMetadataExtras
import nes.app.util.yearString
import nes.networking.phishin.model.Show

@Composable
fun ShowScreen(
    viewModel: ShowViewModel = hiltViewModel(),
    upClick: () -> Unit,
    onMiniPlayerClick: (title: String) -> Unit
) {
    val showState by viewModel.show.collectAsState()
    val appBarTitle by viewModel.appBarTitle.collectAsState()

    NesScaffold(
        title = appBarTitle,
        state = showState,
        upClick = upClick
    ) { value ->
        ShowListWithPlayer(
            show = value,
            onMiniPlayerClick = onMiniPlayerClick,
            randomImageUriProvider = { viewModel.randomImageUri }
        )
    }
}

@Composable
fun ShowListWithPlayer(
    show: Show,
    randomImageUriProvider: () -> Uri,
    onMiniPlayerClick: (title: String) -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    LaunchedEffect(show) {
        val items = show.tracks.map {
            MediaItem.Builder()
                .setUri(it.mp3)
                .setMediaId(it.mp3)
                .setMimeType(MimeTypes.AUDIO_MPEG)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setExtras(show.toMetadataExtras())
                        .setArtist("Phish")
                        .setAlbumArtist("Phish")
                        .setAlbumTitle("${show.date.toAlbumFormat()} ${show.venue_name}")
                        .setTitle(it.title)
                        .setRecordingYear(show.date.yearString.toInt())
                        .setArtworkUri(randomImageUriProvider())
                        .build()
                )
                .build()
        }

        viewModel.addMediaItems(items)
    }

    val playerState by viewModel.playerState.collectAsState()

    val currentlyPlayingMediaId = playerState.mediaItem?.mediaId
    val playing = playerState.isPlaying

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
                            for (ic in 0 until viewModel.mediaItemCount) {
                                val m = viewModel.getMediaItemAt(ic)
                                if (m.mediaId == show.tracks.first().mp3) {
                                    viewModel.removeMediaItems(0, ic)
                                    break
                                }
                            }
                        }

                        viewModel.seekTo(i, 0)
                        viewModel.play()
                    } else {
                        viewModel.pause()
                    }
                }
            }
        }
        MiniPlayer(
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
