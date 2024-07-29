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
import nes.app.util.NetworkState
import nes.networking.phishin.PhishInRepository
import nes.networking.phishin.model.Show
import nes.networking.retry
import javax.inject.Inject

@HiltViewModel
class ShowViewModel @Inject constructor(
    private val phishInRepository: PhishInRepository,
    savedStateHandle: SavedStateHandle
): ViewModel() {

    private val showId: Long = checkNotNull(savedStateHandle["id"])
    val venue: String = checkNotNull(savedStateHandle["venue"])

    private val _show: MutableStateFlow<NetworkState<Show, String>> = MutableStateFlow(NetworkState.Loading)
    val show: StateFlow<NetworkState<Show, String>> = _show

    init {
        loadShow()
    }

    private fun loadShow() {
        viewModelScope.launch {
            val state: NetworkState<Show, String> = when(val result = retry { phishInRepository.show(showId.toString()) }) {
                is Failure -> NetworkState.Error("Error Occurred!")
                is Success -> NetworkState.Loaded(result.value)
            }

            _show.emit(state)
        }
    }
}