package nes.app.ui.player

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import nes.app.playback.MediaPlayerContainer
import nes.app.ui.player.PlayerState.NoMedia
import nes.app.util.formatedElapsedTime
import okio.ByteString.Companion.decodeBase64
import javax.inject.Inject

sealed interface PlayerState {

    @Immutable
    data object NoMedia: PlayerState

    @Immutable
    data class MediaLoaded(
        val isPlaying: Boolean,
        val mediaItem: MediaItem,
        val formatedElapsedTime: String,
        val duration: Long,
        val currentPosition: Long,
    ): PlayerState
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val mediaPlayerContainer: MediaPlayerContainer,
    savedStateHandle: SavedStateHandle,
): ViewModel() {

    private lateinit var player: Player

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            viewModelScope.launch {
                _playerState.emit(newState())
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            viewModelScope.launch {
                _playerState.emit(newState())
            }
        }
    }

    private val _playerState: MutableStateFlow<PlayerState> = MutableStateFlow(NoMedia)
    val playerState: StateFlow<PlayerState> = _playerState

    private val _title: String? = savedStateHandle["title"]
    val title: String? = _title?.decodeBase64()?.utf8()

    init {
        viewModelScope.launch {
            flow {
                while(mediaPlayerContainer.mediaPlayer == null) {
                    delay(1)
                }
                emit(checkNotNull(mediaPlayerContainer.mediaPlayer))
            }.collect {
                player = it
                it.addListener(playerListener)

                while (true) {
                    delay(1000)
                    _playerState.emit(newState())
                }
            }
        }
    }

    fun play() = player.play()
    fun pause() = player.pause()
    fun seekToPreviousMediaItem() = player.seekToPreviousMediaItem()
    fun seekToNextMediaItem() = player.seekToNextMediaItem()

    val mediaItemCount: Int get() = player.mediaItemCount
    fun getMediaItemAt(i: Int) = player.getMediaItemAt(i)
    fun removeMediaItems(fromIndex: Int, toIndex: Int) = player.removeMediaItems(fromIndex, toIndex)
    fun seekTo(mediaItemIndex: Int, positionMs: Long) = player.seekTo(mediaItemIndex, positionMs)

    fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    override fun onCleared() {
        player.removeListener(playerListener)
    }

    private fun newState() = when(val cmi = player.currentMediaItem) {
        null -> NoMedia
        else -> PlayerState.MediaLoaded(
            isPlaying = player.isPlaying,
            mediaItem = cmi,
            formatedElapsedTime = player.formatedElapsedTime,
            duration = player.duration,
            currentPosition = player.currentPosition
        )
    }
}