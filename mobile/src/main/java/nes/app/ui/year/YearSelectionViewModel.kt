package nes.app.ui.year

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import nes.app.ui.ApiErrorMessage
import nes.app.util.LCE
import nes.networking.phishin.PhishInRepository
import nes.networking.phishin.model.YearData
import nes.networking.retry
import javax.inject.Inject

@HiltViewModel
class YearSelectionViewModel @Inject constructor(
    private val phishinRepository: PhishInRepository,
    private val apiErrorMessage: ApiErrorMessage
): ViewModel() {

    private val _years: MutableStateFlow<LCE<List<YearData>, Exception>> =
        MutableStateFlow(LCE.Loading)
    val years: StateFlow<LCE<List<YearData>, Exception>> = _years

    init {
        loadYears()
    }

    private fun loadYears() {
        viewModelScope.launch {
            val state = when(val result = retry { phishinRepository.years() }) {
                is Failure -> LCE.Error(
                    userDisplayedMessage = apiErrorMessage.value,
                    error = result.reason
                )
                is Success -> LCE.Content(result.value)
            }

            _years.emit(state)
        }
    }
}
