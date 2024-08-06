package nes.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import nes.app.R
import nes.app.util.albumTitle
import nes.app.util.artworkUri
import nes.app.util.title

@Composable
fun MiniPlayer(
    viewModel: PlayerViewModel = hiltViewModel(),
    onClick: (title: String) -> Unit
) {
    val playerState by viewModel.playerState.collectAsState()

    val currentMediaItem = playerState.mediaItem ?: return
    val playing = playerState.isPlaying
    val elapsedTime = playerState.formatedElapsedTime

    Surface(
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(2.dp)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable {
                    onClick(currentMediaItem.albumTitle)
                },
        ) {

            AsyncImage(
                model = currentMediaItem.artworkUri,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .weight(1f)
            ) {
                Text(
                    text = currentMediaItem.title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = currentMediaItem.albumTitle,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Box(modifier = Modifier.fillMaxHeight()) {
                Text(
                    text = elapsedTime,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            IconButton(
                onClick = {
                    if (playing) {
                        viewModel.pause()
                    } else {
                        viewModel.play()
                    }
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
        }
    }
}

//@Preview
//@Composable
//fun MiniPlayerPreview() {
//    NesTheme {
//        MiniPlayer(musicPlayer = object : Player by stub() {
//            override fun getCurrentMediaItem() = MediaItem.Builder()
//                .setMediaMetadata(
//                    MediaMetadata.Builder()
//                        .setTitle("The Lizards")
//                        .setAlbumTitle("2024/08/03 Deer Creek")
//                        .build()
//                )
//                .build()
//
//            override fun isPlaying(): Boolean = true
//            override fun addListener(listener: Player.Listener) = Unit
//            // should display 1:20
//            override fun getCurrentPosition(): Long = 1000 * 60 * 1 + 20
//        }) {
//            // onClick do nothing
//        }
//    }
//}