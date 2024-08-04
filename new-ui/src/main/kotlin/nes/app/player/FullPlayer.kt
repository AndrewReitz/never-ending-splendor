package nes.app.player

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import nes.app.R
import nes.app.components.CastButton
import nes.app.components.LoadingScreen
import nes.app.components.TopAppBarText
import nes.app.ui.NesTheme
import nes.app.util.artworkUri
import nes.app.util.mediaMetaData
import nes.app.util.stub
import nes.app.util.toShowInfo
import nes.networking.phishin.model.Show

@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Composable
fun FullPlayer(
    player: Player?,
    navigateToShow: (showId: Long, venueName: String) -> Unit,
    upClick: () -> Unit,
) {
    val title = "7/27/2024 Alpine Valley"

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
                actions = { CastButton() }
            )
        }
    ) { innerPadding ->

        val currentMediaItem: MediaItem? by remember { mutableStateOf(player?.currentMediaItem) }

        // todo wireup callbacks

        if (currentMediaItem == null) {
            LoadingScreen()
        } else {
            val mediaItem = checkNotNull(currentMediaItem)

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
                    modifier = Modifier.fillMaxWidth()
                        .background(Color.Cyan),
                    contentScale = ContentScale.Fit,
                    model = currentMediaItem.artworkUri,
                    contentDescription = null,
                )

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.Cyan)
                    ) {

                    }

                    Column(
                        modifier = Modifier
                            .background(Color.Magenta)
                            .padding(8.dp)
                    ) {
                        Text(text = "title")
                        Text(text = "duration")
                    }

                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "previous song"
                        )
                    }
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "play"
                        )
                    }
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "skip to next song"
                        )
                    }
                }
            }
        }
    }
}

@ExperimentalMaterial3Api
@Preview(showBackground = true)
@Composable
fun NesPlayerPreview() {
    NesTheme {
//        FullPlayer(player = object : Player by stub() {
//            override fun getCurrentMediaItem() = MediaItem.Builder()
//                .setMediaMetadata(
//                    MediaMetadata.Builder()
//                        .build()
//                )
//                .build()
//        }) {
//
//        }
    }
}