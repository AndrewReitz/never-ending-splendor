package nes.app.ui.show

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
import nes.app.util.toSimpleFormat
import nes.networking.phishin.model.Show

@UnstableApi
@Composable
fun ShowSelectionScreen(
    viewModel: ShowSelectionViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    navigateUpClick: () -> Unit,
    onShowClicked: (showId: Long, venue: String) -> Unit,
    onMiniPlayerClick: (title: String) -> Unit,
) {
    val playerState by playerViewModel.playerState.collectAsState()
    val state: LCE<List<Show>, Exception> by viewModel.shows.collectAsState()

    ShowSelectionScreen(
        screenTitle = viewModel.showYear,
        state = state,
        playerState = playerState,
        navigateUpClick = navigateUpClick,
        onShowClicked = onShowClicked,
        onMiniPlayerClick = onMiniPlayerClick,
        onPauseAction = playerViewModel::pause,
        onPlayAction = playerViewModel::play,
        actions = { CastButton() }
    )
}

@Composable
fun ShowSelectionScreen(
    screenTitle: String,
    state: LCE<List<Show>, Exception>,
    playerState: PlayerState,
    navigateUpClick: () -> Unit,
    onShowClicked: (showId: Long, venue: String) -> Unit,
    onMiniPlayerClick: (title: String) -> Unit,
    onPauseAction: () -> Unit,
    onPlayAction: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    val selectionData = state.mapCollection {
        SelectionData(
            title = it.venue_name,
            subtitle = it.date.toSimpleFormat()
        ) {
            onShowClicked(it.id, it.venue_name)
        }
    }

    SelectionScreen(
        title = screenTitle,
        state = selectionData,
        upClick = navigateUpClick,
        onMiniPlayerClick = onMiniPlayerClick,
        playerState = playerState,
        onPauseAction = onPauseAction,
        onPlayAction = onPlayAction,
        actions = actions
    )
}

