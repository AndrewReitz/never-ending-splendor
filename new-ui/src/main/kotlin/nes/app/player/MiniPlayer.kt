package nes.app.player

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nes.app.R

@Composable
fun MiniPlayer(
    mediaController: MediaController?,
    onClick: () -> Unit
) {

    if (mediaController == null) {
        return
    }

    var currentlyPlayingTrackName by remember {
        mutableStateOf(mediaController.currentMediaItem.title)
    }
    var playing by remember { mutableStateOf(mediaController.isPlaying) }
    val playerListener by remember {
        mutableStateOf(
            object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    currentlyPlayingTrackName = mediaItem.title
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

    DisposableEffect(Unit) {
        onDispose {
            mediaController.removeListener(playerListener)
        }
    }

    LaunchedEffect(Unit) {
        mediaController.addListener(playerListener)
    }

    if (mediaController.currentMediaItem == null) {
        return
    }

    val scope = rememberCoroutineScope()
    var duration by remember { mutableStateOf( "--:--") }
    LaunchedEffect(Unit) {
        scope.launch {
            while(true) {
                delay(1000)
                duration = DateUtils.formatElapsedTime(mediaController.currentPosition / 1000)
            }
        }
    }

    // todo show "album" too

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color.Cyan)
            .clickable(onClick = onClick),
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
                text = currentlyPlayingTrackName,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = duration,
                style = MaterialTheme.typography.bodySmall
            )
        }

        IconButton(
            onClick = {
                if (mediaController.isPlaying) {
                    mediaController.pause()
                } else {
                    mediaController.play()
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

private val MediaItem?.title: String get() = this?.mediaMetadata?.title?.toString() ?: "--"