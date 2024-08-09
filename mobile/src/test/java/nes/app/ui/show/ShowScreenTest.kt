package nes.app.ui.show

import nes.app.showContent
import nes.app.showingPlayerState
import nes.app.ui.PaparazziTest
import nes.app.util.LCE
import nes.networking.phishin.model.Show
import okio.IOException
import org.junit.Test

class ShowScreenTest : PaparazziTest() {
    @Test
    fun error() {
        snapshot(
            state = LCE.Error(
                userDisplayedMessage = "There was an error getting data from Phish.in, check your network connection and try again.",
                error = IOException("An error occurred")
            )
        )
    }

    @Test
    fun loading() {
        snapshot(state = LCE.Loading)
    }

    @Test
    fun content() {
        snapshot(state = showContent)
    }

    private fun snapshot(
        state: LCE<Show, Exception>,
    ) {
        paparazzi.snapshot {
            ShowScreen(
                state = state,
                playerState = showingPlayerState,
                appBarTitle = "2021/08/08 Ruoff Music Center",
                onMiniPlayerClick = {},
                onPauseAction = {},
                onPlayAction = {},
                actions = {},
                onRowClick = {},
                upClick = {}
            )
        }
    }
}