package nes.app.player

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.ui.PlayerView
import androidx.media3.ui.PlayerView.SHOW_BUFFERING_WHEN_PLAYING

@OptIn(UnstableApi::class)
@Composable
fun NesPlayer(
    modifier: Modifier = Modifier,
    mediaController: MediaController?
) {
    // todo make not suck

    AndroidView(
        modifier = modifier
            .fillMaxWidth(),
        factory = {
            PlayerView(it).apply {
                this.player = mediaController
                this.controllerHideOnTouch = false
                this.setShowBuffering(SHOW_BUFFERING_WHEN_PLAYING)
                this.showController()
                this.controllerAutoShow = false
            }
        }
    )
}