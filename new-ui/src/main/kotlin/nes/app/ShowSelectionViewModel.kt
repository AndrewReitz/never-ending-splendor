package nes.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import nes.networking.phishin.PhishInRepository
import nes.networking.phishin.model.Show
import nes.networking.retry
import javax.inject.Inject

@HiltViewModel
class ShowSelectionViewModel @Inject constructor(
    private val phishinRepository: PhishInRepository,
    savedStateHandle: SavedStateHandle
): ViewModel() {

    private val showYear: String = checkNotNull(savedStateHandle["year"])

    private val _shows: MutableStateFlow<NetworkState<List<Show>, String>> = MutableStateFlow(NetworkState.Loading)
    val shows: StateFlow<NetworkState<List<Show>, String>> = _shows

    init {
        loadShows()
    }

    private fun loadShows() {
        viewModelScope.launch {
            val state = when(val result = retry { phishinRepository.shows(showYear) }) {
                is Failure -> NetworkState.Error("Error Occurred!")
                is Success -> NetworkState.Loaded(result.value)
            }

            _shows.emit(state)
        }
    }
}