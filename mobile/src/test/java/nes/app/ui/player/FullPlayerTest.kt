package nes.app.ui.player

import app.cash.paparazzi.Paparazzi
import nes.app.noShowPlayerState
import nes.app.showingPlayerState
import nes.app.ui.PaparazziNightTest
import nes.app.ui.PaparazziTest
import org.junit.Test

class FullPlayerTest : PaparazziTest() {

    @Test
    fun `no media`() {
        paparazzi.snapshot(noShowPlayerState)
    }

    @Test
    fun playing() {
        paparazzi.snapshot(showingPlayerState)
    }

    @Test
    fun paused() {
        paparazzi.snapshot(showingPlayerState.copy(isPlaying = false))
    }
}

class FullPlayerNightTest : PaparazziNightTest() {

    @Test
    fun `no media`() {
        paparazzi.snapshot(noShowPlayerState)
    }

    @Test
    fun playing() {
        paparazzi.snapshot(showingPlayerState)
    }

    @Test
    fun paused() {
        paparazzi.snapshot(showingPlayerState.copy(isPlaying = false))
    }
}

private fun Paparazzi.snapshot(state: PlayerState) {
    snapshot {
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