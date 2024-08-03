package nes.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import nes.app.util.LCE
import nes.app.R
import nes.app.player.MiniPlayer
import nes.app.ui.Rainbow

data class SelectionData(
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionScreen(
    title: String = stringResource(R.string.app_name),
    state: LCE<List<SelectionData>, String>,
    musicPlayer: Player?,
    upClick: (() -> Unit)?,
    onMiniPlayerClick: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    upClick?.let {
                        IconButton(onClick = upClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.navigate_back)
                            )
                        }
                    }
                },
                actions = {
                    CastButton()
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when(state) {
                is LCE.Error -> ErrorScreen(state.error)
                is LCE.Loaded -> Column {
                    SelectionList(
                        Modifier.weight(1f),
                        state.value
                    )
                    MiniPlayer(
                        musicPlayer = musicPlayer,
                        onClick = onMiniPlayerClick
                    )
                }
                LCE.Loading -> LoadingScreen()
            }
        }
    }
}

@Composable
fun SelectionList(
    modifier: Modifier = Modifier,
    data: List<SelectionData>,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        itemsIndexed(data) { i, (title, subtitle, onClick) ->
            SelectionRow(
                title = title,
                subtitle = subtitle,
                boxColor = Rainbow[i % Rainbow.size],
                onClick = onClick
            )
        }
    }
}

@Composable
fun SelectionRow(
    title: String,
    subtitle: String,
    boxColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .fillMaxWidth()
            .clickable {
                onClick()
            }
    ) {
        Box(modifier = Modifier
            .width(80.dp)
            .height(96.dp)
            .background(boxColor))

        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
