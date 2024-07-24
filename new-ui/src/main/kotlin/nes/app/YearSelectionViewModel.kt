package nes.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import nes.networking.phishin.PhishInRepository
import nes.networking.phishin.model.YearData
import nes.networking.retry
import javax.inject.Inject

@HiltViewModel
class YearSelectionViewModel @Inject constructor(
    private val phishinRepository: PhishInRepository
): ViewModel() {

    private val _years: MutableStateFlow<NetworkState<List<YearData>, String>> =
        MutableStateFlow(NetworkState.Loading)
    val years: StateFlow<NetworkState<List<YearData>, String>> = _years

    init {
        loadYears()
    }

    private fun loadYears() {
        viewModelScope.launch {
            val state = when(val result = retry { phishinRepository.years() }) {
                is Failure -> NetworkState.Error("Error occurred!")
                is Success -> NetworkState.Loaded(result.value)
            }

            _years.emit(state)
        }
    }
}