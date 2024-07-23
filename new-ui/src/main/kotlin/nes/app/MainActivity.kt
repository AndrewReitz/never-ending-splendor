package nes.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import nes.app.ui.NesTheme
import nes.networking.phishin.PhishInRepository
import nes.networking.phishin.model.YearData
import nes.networking.retry
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            NesTheme {
                NesApp()
            }
        }
    }
}

sealed class Screen(
    val route: String,
    val navArguments: List<NamedNavArgument> = emptyList()
) {
    data object YearSelection : Screen("yearSelection")
}

@Composable
fun NesApp() {
    val navController = rememberNavController()
    NesNavController(navController = navController)
}

@Composable
fun NesNavController(
    navController: NavHostController
) {
    NavHost(navController = navController, startDestination = Screen.YearSelection.route) {
        composable(route = Screen.YearSelection.route) { YearSelectionScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearSelectionScreen(
    viewModel: YearSelectionViewModel = hiltViewModel()
) {
    val state by viewModel.years.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(text = "App Name Todo") }) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when(val s = state) {
                is YearSelectionViewModel.YearSelectionState.Error -> ErrorScreen(s.message)
                is YearSelectionViewModel.YearSelectionState.Loaded -> LoadingScreen()
                YearSelectionViewModel.YearSelectionState.Loading -> LoadingScreen()
            }
        }
    }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.then(Modifier.fillMaxSize())) {
        CircularProgressIndicator(
            modifier = Modifier
                .width(64.dp)
                .align(Alignment.Center),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
fun ErrorScreen(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.then(Modifier.fillMaxSize())) {
        Text(text = message)
    }
}

@HiltViewModel
class YearSelectionViewModel @Inject constructor(
    private val phishinRepository: PhishInRepository,
    private val savedStateHandle: SavedStateHandle
): ViewModel() {

    sealed interface YearSelectionState {
        data object Loading: YearSelectionState
        data class Loaded(val years: List<YearData>): YearSelectionState
        data class Error(val message: String): YearSelectionState
    }

    private val _years: MutableStateFlow<YearSelectionState> = MutableStateFlow(YearSelectionState.Loading)
    val years: StateFlow<YearSelectionState> = _years

    init {
        loadYears()
    }

    fun loadYears() {
        viewModelScope.launch {
            val state = when(val result = retry { phishinRepository.years() } ) {
                is Failure -> YearSelectionState.Error("Error occurred!")
                is Success -> YearSelectionState.Loaded(result.value)
            }

            _years.emit(state)
        }
    }
}