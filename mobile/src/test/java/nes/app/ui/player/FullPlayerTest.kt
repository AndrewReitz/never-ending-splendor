package nes.app.ui.player

import nes.app.noShowPlayerState
import nes.app.showingPlayerState
import nes.app.ui.PaparazziTest
import org.junit.Test

class FullPlayerTest : PaparazziTest() {

    @Test
    fun `no media`() {
        snapshot(noShowPlayerState)
    }

    @Test
    fun playing() {
        snapshot(showingPlayerState)
    }

    @Test
    fun paused() {
        snapshot(showingPlayerState.copy(isPlaying = false))
    }

    private fun snapshot(state: PlayerState) {
        paparazzi.snapshot {
            FullPlayer(
                playerState = state,
                title = "2021/08/08 Deer Creek Music Center",
                navigateToShow = { _, _ -> },
                upClick = { },
                seekTo = {},
                seekToPreviousMediaItem = { },
                seekToNextMediaItem = { },
                onPause = {},
                onPlay = {},
                actions = {}
            )
        }
    }
}