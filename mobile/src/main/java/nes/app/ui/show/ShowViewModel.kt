package nes.app.ui.show

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import nes.app.playback.MediaPlayerContainer
import nes.app.ui.ApiErrorMessage
import nes.app.util.Images
import nes.app.util.LCE
import nes.app.util.map
import nes.app.util.retryUntilSuccessful
import nes.app.util.showTitle
import nes.app.util.toAlbumFormat
import nes.app.util.toMetadataExtras
import nes.app.util.yearString
import nes.networking.phishin.PhishInRepository
import nes.networking.phishin.model.Show
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ShowViewModel @Inject constructor(
    private val phishInRepository: PhishInRepository,
    private val images: Images,
    private val mediaPlayerContainer: MediaPlayerContainer,
    private val apiErrorMessage: ApiErrorMessage,
    savedStateHandle: SavedStateHandle
): ViewModel() {

    private val showId: Long = checkNotNull(savedStateHandle["id"])
    private val venue: String = checkNotNull(savedStateHandle["venue"])

    private val _appBarTitle: MutableStateFlow<String> = MutableStateFlow(venue)
    val appBarTitle: StateFlow<String> = _appBarTitle

    private val _show: MutableStateFlow<LCE<Show, Throwable>> = MutableStateFlow(LCE.Loading)
    val show: StateFlow<LCE<Show, Throwable>> = _show

    init {
        loadShow()
    }

    @OptIn(UnstableApi::class)
    private fun loadShow() {
        viewModelScope.launch {
            val state = retryUntilSuccessful(
                action = { phishInRepository.show(showId.toString()) },
                onErrorAfter3SecondsAction = { error ->
                    Timber.d(error, "Error retrieving show")
                    _show.emit(
                        LCE.Error(
                            userDisplayedMessage = apiErrorMessage.value,
                            error = error
                        )
                    )
                }
            ).map { show ->
                val items = show.tracks.map { track ->
                    MediaItem.Builder()
                        .setUri(track.mp3)
                        .setMediaId(track.mp3)
                        .setMimeType(MimeTypes.AUDIO_MPEG)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setExtras(show.toMetadataExtras())
                                .setArtist("Phish")
                                .setAlbumArtist("Phish")
                                .setAlbumTitle(show.showTitle)
                                .setTitle(track.title)
                                .setRecordingYear(show.date.yearString.toInt())
                                .setArtworkUri(images.randomImageUrl.toUri())
                                .setDurationMs(track.duration)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                                .setIsPlayable(true)
                                .setIsBrowsable(false)
                                .build()
                        )
                        .build()
                }

                checkNotNull(mediaPlayerContainer.mediaPlayer).addMediaItems(items)
                viewModelScope.launch {
                    _appBarTitle.emit("${show.date.toAlbumFormat()} ${show.venue_name}")
                }

                show
            }

            _show.emit(state)
        }
    }
}