package never.ending.splendor.app.newui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import nes.networking.phishin.PhishInRepository
import nes.networking.phishin.model.Show
import nes.networking.phishin.model.YearData
import nes.networking.retry

sealed interface AppState {
    data object InitialState: AppState
    data object Loading: AppState

    data class Years(
        val data: List<YearData>
    ): AppState

    data class Shows(
        val data: List<Show>
    ): AppState

    data class Error(val message: String): AppState
}

class NewUiViewModel(
    private val phishinRepository: PhishInRepository,
) : ViewModel() {
    private val _appState: MutableStateFlow<AppState> = MutableStateFlow(AppState.InitialState)
    val appState: StateFlow<AppState> = _appState

    init {
        loadYears()
    }

    fun loadYears() {
        viewModelScope.launch {
            val state = when(val result = retry { phishinRepository.years() } ) {
                is Failure -> AppState.Error("Error occurred!")
                is Success -> AppState.Years(result.value)
            }

            _appState.emit(state)
        }
    }

    fun loadShows(year: String) {
        viewModelScope.launch {
            _appState.emit(AppState.Loading)

            val state = when (val result = retry { phishinRepository.shows(year) } ) {
                is Failure -> AppState.Error("Error occurred!")
                is Success -> AppState.Shows(result.value)
            }

            _appState.emit(state)
        }
    }
}