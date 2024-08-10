package nes.app.ui.year

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import nes.app.ui.components.CastButton
import nes.app.ui.components.SelectionData
import nes.app.ui.components.SelectionScreen
import nes.app.ui.player.PlayerState
import nes.app.ui.player.PlayerViewModel
import nes.app.util.LCE
import nes.app.util.mapCollection
import nes.networking.phishin.model.YearData

@OptIn(UnstableApi::class)
@Composable
fun YearSelectionScreen(
    viewModel: YearSelectionViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    onYearClicked: (year: String) -> Unit,
    onMiniPlayerClick: (title: String) -> Unit,
) {
    val state: LCE<List<YearData>, Exception> by viewModel.years.collectAsState()
    val playerState by playerViewModel.playerState.collectAsState()

    YearSelectionScreen(
        yearData = state,
        onYearClicked = onYearClicked,
        onMiniPlayerClick = onMiniPlayerClick,
        playerState = playerState,
        onPauseAction = playerViewModel::pause,
        onPlayAction = playerViewModel::play,
        actions = { CastButton() }
    )
}

@Composable
fun YearSelectionScreen(
    yearData: LCE<List<YearData>, Exception>,
    onYearClicked: (year: String) -> Unit,
    onMiniPlayerClick: (title: String) -> Unit,
    playerState: PlayerState,
    onPauseAction: () -> Unit,
    onPlayAction: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    val selectionData = yearData.mapCollection {
        SelectionData(title = it.date, subtitle = "${it.show_count} shows") {
            onYearClicked(it.date)
        }
    }

    SelectionScreen(
        state = selectionData,
        upClick = null,
        onMiniPlayerClick = onMiniPlayerClick,
        playerState = playerState,
        onPauseAction = onPauseAction,
        onPlayAction = onPlayAction,
        actions = actions
    )
}
