package nes.app.ui.show

import app.cash.paparazzi.Paparazzi
import nes.app.noShowPlayerState
import nes.app.showListContent
import nes.app.showingPlayerState
import nes.app.ui.PaparazziTest
import nes.app.ui.player.PlayerState
import nes.app.util.LCE
import nes.networking.phishin.model.Show
import okio.IOException
import org.junit.Test

class ShowSelectionScreenTest : PaparazziTest() {

    @Test
    fun error() {
        paparazzi.snapshot(
            state = LCE.Error(
                userDisplayedMessage = "There was an error getting data from Phish.in, check your network connection and try again.",
                error = IOException("An error occurred")
            )
        )
    }

    @Test
    fun loading() {
        paparazzi.snapshot(state = LCE.Loading)
    }

    @Test
    fun content() {
        paparazzi.snapshot(state = showListContent)
    }

    @Test
    fun `content with mini player`() {
        paparazzi.snapshot(
            state = showListContent,
            playerState = showingPlayerState
        )
    }
}

private fun Paparazzi.snapshot(
    state: LCE<List<Show>, Exception>,
    playerState: PlayerState = noShowPlayerState
) {
    snapshot {
        ShowSelectionScreen(
            screenTitle = "2001",
            state = state,
            playerState = playerState,
            navigateUpClick = {},
            onShowClicked = { _, _ -> },
            onMiniPlayerClick = {},
            onPauseAction = {},
            onPlayAction = {},
            actions = {}
        )
    }
}
