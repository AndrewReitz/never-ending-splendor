package nes.app.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nes.app.noShowPlayerState
import nes.app.showingPlayerState
import nes.app.ui.PaparazziTest
import org.junit.Test

class MiniPlayerTest : PaparazziTest() {

    @Test
    fun `should not show mini player when not playing`() {
        snapshot(noShowPlayerState)
    }

    @Test
    fun `show mini player paused`() {
        snapshot(showingPlayerState.copy(isPlaying = false))
    }

    @Test
    fun `show mini player playing`() {
        snapshot(showingPlayerState)
    }

    private fun snapshot(state: PlayerState) {
        paparazzi.snapshot {
            Box(
                modifier = Modifier.fillMaxSize()
                    .height(64.dp)
            ) {
                MiniPlayer(
                    playerState = state,
                    onClick = {},
                    onPlayAction = {},
                    onPauseAction = {}
                )
            }
        }
    }
}