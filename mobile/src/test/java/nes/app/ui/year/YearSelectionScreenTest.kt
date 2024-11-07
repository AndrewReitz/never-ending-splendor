package nes.app.ui.year

import app.cash.paparazzi.Paparazzi
import nes.app.noShowPlayerState
import nes.app.showingPlayerState
import nes.app.ui.PaparazziNightTest
import nes.app.ui.PaparazziTest
import nes.app.ui.player.PlayerState
import nes.app.ui.player.PlayerViewModel
import nes.app.util.LCE
import nes.app.yearsContent
import nes.networking.phishin.model.YearData
import okio.IOException
import org.junit.Test

class YearSelectionScreenTest : PaparazziTest() {

    @Test
    fun error() {
        val data = LCE.Error(
            userDisplayedMessage = "There was an error getting data from Phish.in, check your network connection and try again.",
            error = IOException()
        )

        paparazzi.snapshot(data)
    }

    @Test
    fun loading() {
        paparazzi.snapshot(LCE.Loading)
    }

    @Test
    fun content() {
        paparazzi.snapshot(
            yearData = yearsContent
        )
    }

    @Test
    fun `content with mini player`() {
        paparazzi.snapshot(
            yearData = yearsContent,
            playerState = showingPlayerState
        )
    }
}

class YearSelectionScreenNightTest : PaparazziNightTest() {

    @Test
    fun error() {
        val data = LCE.Error(
            userDisplayedMessage = "There was an error getting data from Phish.in, check your network connection and try again.",
            error = IOException()
        )

        paparazzi.snapshot(data)
    }

    @Test
    fun loading() {
        paparazzi.snapshot(LCE.Loading)
    }

    @Test
    fun content() {
        paparazzi.snapshot(
            yearData = yearsContent
        )
    }

    @Test
    fun `content with mini player`() {
        paparazzi.snapshot(
            yearData = yearsContent,
            playerState = showingPlayerState
        )
    }
}

private fun Paparazzi.snapshot(
    yearData: LCE<List<YearData>, Exception>,
    playerState: PlayerState = noShowPlayerState
) {
    snapshot {
        YearSelectionScreen(
            yearData = yearData,
            playerState = playerState,
            onYearClicked = {},
            onMiniPlayerClick = {},
            onPlayAction = {},
            onPauseAction = {},
            actions = {}
        )
    }
}
