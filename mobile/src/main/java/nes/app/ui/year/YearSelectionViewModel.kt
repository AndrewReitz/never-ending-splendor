package nes.app.ui.year

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
import nes.networking.phishin.model.YearData
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class YearSelectionViewModel @Inject constructor(
    private val phishinRepository: PhishInRepository,
    private val apiErrorMessage: ApiErrorMessage
): ViewModel() {

    private val _years: MutableStateFlow<LCE<List<YearData>, Throwable>> =
        MutableStateFlow(LCE.Loading)
    val years: StateFlow<LCE<List<YearData>, Throwable>> = _years

    init {
        loadYears()
    }

    private fun loadYears() {
        viewModelScope.launch {
            val state = retryUntilSuccessful(
                action = { phishinRepository.years() },
                onErrorAfter3SecondsAction = { error ->
                    Timber.d(error, "Error loading years.")
                    _years.emit(
                        LCE.Error(
                            userDisplayedMessage = apiErrorMessage.value,
                            error = error
                        )
                    )
                }
            )

            _years.emit(state)
        }
    }
}
