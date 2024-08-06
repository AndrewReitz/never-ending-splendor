package nes.app.ui.year

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player
import nes.app.ui.components.SelectionData
import nes.app.ui.components.SelectionScreen
import nes.app.util.mapCollection

@Composable
fun YearSelectionScreen(
    viewModel: YearSelectionViewModel = hiltViewModel(),
    onYearClicked: (year: String) -> Unit,
    onMiniPlayerClick: (title: String) -> Unit,
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
        onMiniPlayerClick = onMiniPlayerClick,
    )
}
