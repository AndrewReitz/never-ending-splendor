package nes.app.year

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.session.MediaController
import nes.app.components.SelectionData
import nes.app.components.SelectionScreen
import nes.app.util.mapCollection

@Composable
fun YearSelectionScreen(
    viewModel: YearSelectionViewModel = hiltViewModel(),
    mediaController: MediaController?,
    onYearClicked: (year: String) -> Unit,
    onMiniPlayerClick: () -> Unit,
) {
    val state by viewModel.years.collectAsState()
    val selectionData = state.mapCollection {
        SelectionData(title = it.date, subtitle = "${it.show_count} shows") {
            onYearClicked(it.date)
        }
    }

    SelectionScreen(
        state = selectionData,
        upClick = null,
        mediaController = mediaController,
        onMiniPlayerClick = onMiniPlayerClick,
    )
}
