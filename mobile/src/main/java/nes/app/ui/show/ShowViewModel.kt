package nes.app.ui.show

import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import nes.app.playback.MediaPlayerContainer
import nes.app.util.Images
import nes.app.util.LCE
import nes.app.util.showTitle
import nes.app.util.toAlbumFormat
import nes.app.util.toMetadataExtras
import nes.app.util.yearString
import nes.networking.phishin.PhishInRepository
import nes.networking.phishin.model.Show
import nes.networking.retry
import javax.inject.Inject

@HiltViewModel
class ShowViewModel @Inject constructor(
    private val phishInRepository: PhishInRepository,
    private val images: Images,
    private val mediaPlayerContainer: MediaPlayerContainer,
    savedStateHandle: SavedStateHandle
): ViewModel() {

    private val showId: Long = checkNotNull(savedStateHandle["id"])
    private val venue: String = checkNotNull(savedStateHandle["venue"])

    private val _appBarTitle: MutableStateFlow<String> = MutableStateFlow(venue)
    val appBarTitle: StateFlow<String> = _appBarTitle

    private val _show: MutableStateFlow<LCE<Show, Exception>> = MutableStateFlow(LCE.Loading)
    val show: StateFlow<LCE<Show, Exception>> = _show

    init {
        loadShow()
    }

    @OptIn(UnstableApi::class)
    private fun loadShow() {
        viewModelScope.launch {
            val state: LCE<Show, Exception> = when(val result = retry { phishInRepository.show(showId.toString()) }) {
                is Failure -> LCE.Error(userDisplayedMessage = "Error Occurred!", error = result.reason)
                is Success -> {
                    val show = result.value

                    val items = show.tracks.map {
                        MediaItem.Builder()
                            .setUri(it.mp3)
                            .setMediaId(it.mp3)
                            .setMimeType(MimeTypes.AUDIO_MPEG)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setExtras(show.toMetadataExtras())
                                    .setArtist("Phish")
                                    .setAlbumArtist("Phish")
                                    .setAlbumTitle(show.showTitle)
                                    .setTitle(it.title)
                                    .setRecordingYear(show.date.yearString.toInt())
                                    .setArtworkUri(images.randomImageUrl.toUri())
                                    .setDurationMs(it.duration)
                                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                                    .setIsPlayable(true)
                                    .setIsBrowsable(false)
                                    .build()
                            )
                            .build()
                    }

                    checkNotNull(mediaPlayerContainer.mediaPlayer).addMediaItems(items)

                    _appBarTitle.emit("${show.date.toAlbumFormat()} ${show.venue_name}")
                    LCE.Content(show)
                }
            }

            _show.emit(state)
        }
    }
}