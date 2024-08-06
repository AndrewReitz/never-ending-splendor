package nes.app.ui.show

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player
import nes.app.ui.components.SelectionData
import nes.app.ui.components.SelectionScreen
import nes.app.util.mapCollection
import nes.app.util.toSimpleFormat

@Composable
fun ShowSelectionScreen(
    viewModel: ShowSelectionViewModel = hiltViewModel(),
    navigateUpClick: () -> Unit,
    onShowClicked: (showId: Long, venue: String) -> Unit,
    onMiniPlayerClick: (title: String) -> Unit,
) {
    val state by viewModel.shows.collectAsState()
    val selectionData = state.mapCollection {
        SelectionData(
            title = it.venue_name,
            subtitle = it.date.toSimpleFormat()
        ) {
            onShowClicked(it.id, it.venue_name)
        }
    }

    SelectionScreen(
        title = viewModel.showYear,
        state = selectionData,
        upClick = navigateUpClick,
        onMiniPlayerClick = onMiniPlayerClick
    )
}
