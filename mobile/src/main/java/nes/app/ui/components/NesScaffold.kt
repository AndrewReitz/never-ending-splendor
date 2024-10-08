package nes.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nes.app.R
import nes.app.util.LCE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> NesScaffold(
    title: String,
    state: LCE<T, Any>,
    upClick: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable (value: T) -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { TopAppBarText(title) },
                navigationIcon = {
                    upClick?.let {
                        IconButton(onClick = upClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.navigate_back)
                            )
                        }

                    } ?: run {
                        Image(
                            modifier = Modifier.size(48.dp),
                            painter = painterResource(id = R.drawable.app_icon),
                            contentDescription = null
                        )
                    }
                },
                actions = actions
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when(state) {
                is LCE.Error -> ErrorScreen(state.userDisplayedMessage)
                is LCE.Content -> content(state.value)
                LCE.Loading -> LoadingScreen()
            }
        }
    }
}
