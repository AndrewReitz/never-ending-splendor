package nes.app.ui.show

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import nes.app.ui.ApiErrorMessage
import nes.app.util.LCE
import nes.app.util.retryUntilSuccessful
import nes.networking.phishin.PhishInRepository
import nes.networking.phishin.model.Show
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ShowSelectionViewModel @Inject constructor(
    private val phishinRepository: PhishInRepository,
    private val apiErrorMessage: ApiErrorMessage,
    savedStateHandle: SavedStateHandle
): ViewModel() {

    val showYear: String = checkNotNull(savedStateHandle["year"])

    private val _shows: MutableStateFlow<LCE<List<Show>, Throwable>> = MutableStateFlow(LCE.Loading)
    val shows: StateFlow<LCE<List<Show>, Throwable>> = _shows

    init {
        loadShows()
    }

    private fun loadShows() {
        viewModelScope.launch {
            val state = retryUntilSuccessful(
                action = { phishinRepository.shows(showYear) },
                onErrorAfter3SecondsAction = { error ->
                    Timber.d(error, "Error retrieving shows")
                    _shows.emit(
                        LCE.Error(
                            userDisplayedMessage = apiErrorMessage.value,
                            error = error
                        )
                    )
                }
            )
            _shows.emit(state)
        }
    }
}
