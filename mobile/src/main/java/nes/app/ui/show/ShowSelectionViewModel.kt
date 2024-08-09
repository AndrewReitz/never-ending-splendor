package nes.app.ui.show

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import nes.app.util.LCE
import nes.networking.phishin.PhishInRepository
import nes.networking.phishin.model.Show
import nes.networking.retry
import javax.inject.Inject

@HiltViewModel
class ShowSelectionViewModel @Inject constructor(
    private val phishinRepository: PhishInRepository,
    savedStateHandle: SavedStateHandle
): ViewModel() {

    val showYear: String = checkNotNull(savedStateHandle["year"])

    private val _shows: MutableStateFlow<LCE<List<Show>, Exception>> = MutableStateFlow(LCE.Loading)
    val shows: StateFlow<LCE<List<Show>, Exception>> = _shows

    init {
        loadShows()
    }

    private fun loadShows() {
        viewModelScope.launch {
            val state = when(val result = retry { phishinRepository.shows(showYear) }) {
                is Failure -> LCE.Error(
                    userDisplayedMessage = "There was an error loading data from Phish.in",
                    error = result.reason
                )
                is Success -> LCE.Content(result.value)
            }

            _shows.emit(state)
        }
    }
}