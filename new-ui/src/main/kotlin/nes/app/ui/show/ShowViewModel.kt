package nes.app.ui.show

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import nes.app.util.Images
import nes.app.util.LCE
import nes.app.util.toAlbumFormat
import nes.networking.phishin.PhishInRepository
import nes.networking.phishin.model.Show
import nes.networking.retry
import javax.inject.Inject

@HiltViewModel
class ShowViewModel @Inject constructor(
    private val phishInRepository: PhishInRepository,
    private val images: Images,
    savedStateHandle: SavedStateHandle
): ViewModel() {

    private val showId: Long = checkNotNull(savedStateHandle["id"])
    private val venue: String = checkNotNull(savedStateHandle["venue"])

    private val _appBarTitle: MutableStateFlow<String> = MutableStateFlow(venue)
    val appBarTitle: StateFlow<String> = _appBarTitle

    private val _show: MutableStateFlow<LCE<Show, Exception>> = MutableStateFlow(LCE.Loading)
    val show: StateFlow<LCE<Show, Exception>> = _show

    val randomImageUri: Uri get() = images.randomImageUrl.toUri()

    init {
        loadShow()
    }

    private fun loadShow() {
        viewModelScope.launch {
            val state: LCE<Show, Exception> = when(val result = retry { phishInRepository.show(showId.toString()) }) {
                is Failure -> LCE.Error(userDisplayedMessage = "Error Occurred!", error = result.reason)
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