package nes.app.show

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
import nes.app.util.toAlbumFormat
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
    private val venue: String = checkNotNull(savedStateHandle["venue"])

    private val _appBarTitle: MutableStateFlow<String> = MutableStateFlow(venue)
    val appBarTitle: StateFlow<String> = _appBarTitle

    private val _show: MutableStateFlow<LCE<Show, String>> = MutableStateFlow(LCE.Loading)
    val show: StateFlow<LCE<Show, String>> = _show

    init {
        loadShow()
    }

    private fun loadShow() {
        viewModelScope.launch {
            val state: LCE<Show, String> = when(val result = retry { phishInRepository.show(showId.toString()) }) {
                is Failure -> LCE.Error("Error Occurred!")
                is Success -> {
                    val value = result.value
                    _appBarTitle.emit("${value.date.toAlbumFormat()} ${value.venue_name}")
                    LCE.Loaded(value)
                }
            }

            _show.emit(state)
        }
    }
}