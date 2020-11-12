package never.ending.splendor.app.playback

import android.net.Uri
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.common.images.WebImage
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException
import never.ending.splendor.app.model.MusicProvider
import never.ending.splendor.app.model.MusicProviderSource
import never.ending.splendor.app.utils.MediaIdHelper.musicId
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

/**
 * An implementation of Playback that talks to Cast.
 */
class CastPlayback(
    private val mMusicProvider: MusicProvider,
    private val videoCastManager: VideoCastManager
) : Playback {

    private val castConsumer: VideoCastConsumerImpl = object : VideoCastConsumerImpl() {
        override fun onRemoteMediaPlayerMetadataUpdated() {
            Timber.d("onRemoteMediaPlayerMetadataUpdated")
            setMetadataFromRemote()
        }

        override fun onRemoteMediaPlayerStatusUpdated() {
            Timber.d("onRemoteMediaPlayerStatusUpdated")
            updatePlaybackState()
        }
    }

    /** The current PlaybackState */
    override var state: Int = 0
    override val isConnected: Boolean = videoCastManager.isConnected

    override val isPlaying: Boolean
        get() {
            try {
                return videoCastManager.isConnected &&
                    videoCastManager.isRemoteMediaPlaying
            } catch (e: TransientNetworkDisconnectionException) {
                Timber.e(e, "Exception calling isRemoteMoviePlaying")
            } catch (e: NoConnectionException) {
                Timber.e(e, "Exception calling isRemoteMoviePlaying")
            }
            return false
        }

    override var currentStreamPosition: Int = 0
        get() {
            if (!videoCastManager.isConnected) {
                return currentPosition
            }
            try {
                return videoCastManager.currentMediaPosition.toInt()
            } catch (e: TransientNetworkDisconnectionException) {
                Timber.e(e, "Exception getting media position")
            } catch (e: NoConnectionException) {
                Timber.e(e, "Exception getting media position")
            }
            return -1
        }

    /** Callback for making completion/error calls on  */
    override var callback: Playback.Callback = Playback.Callback.EMPTY

    @Volatile
    var currentPosition = 0

    @Volatile
    override var currentMediaId: String? = null

    override val supportsGapless = false

    override fun start() {
        videoCastManager.addVideoCastConsumer(castConsumer)
    }

    override fun stop(notifyListeners: Boolean) {
        videoCastManager.removeVideoCastConsumer(castConsumer)
        state = PlaybackStateCompat.STATE_STOPPED
        if (notifyListeners) {
            callback.onPlaybackStatusChanged(state)
        }
    }

    override fun updateLastKnownStreamPosition() {
        currentPosition = currentStreamPosition
    }

    override fun playNext(item: MediaSessionCompat.QueueItem): Boolean = false

    override fun play(item: MediaSessionCompat.QueueItem) {
        try {
            loadMedia(item.description.mediaId, true)
            state = PlaybackStateCompat.STATE_BUFFERING
            callback.onPlaybackStatusChanged(state)
        } catch (e: TransientNetworkDisconnectionException) {
            Timber.e(e, "Exception loading media")
            callback.onError(e.message)
        } catch (e: NoConnectionException) {
            Timber.e(e, "Exception loading media")
            callback.onError(e.message)
        } catch (e: JSONException) {
            Timber.e(e, "Exception loading media")
            callback.onError(e.message)
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Exception loading media")
            callback.onError(e.message)
        }
    }

    override fun pause() {
        try {
            val manager = videoCastManager
            if (manager.isRemoteMediaLoaded) {
                manager.pause()
                currentPosition = manager.currentMediaPosition.toInt()
            } else {
                loadMedia(currentMediaId, false)
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception pausing cast playback")
            callback.onError(e.message)
        }
    }

    override fun seekTo(position: Int) {
        if (currentMediaId == null) {
            callback.onError("seekTo cannot be calling in the absence of mediaId.")
            return
        }
        try {
            if (videoCastManager.isRemoteMediaLoaded) {
                videoCastManager.seek(position)
                currentPosition = position
            } else {
                currentPosition = position
                loadMedia(currentMediaId, false)
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception pausing cast playback")
            callback.onError(e.message)
        }
    }

    private fun loadMedia(mediaId: String?, autoPlay: Boolean) {
        val musicId = mediaId?.musicId
        val track = mMusicProvider.getMusic(musicId)
            ?: throw IllegalArgumentException("Invalid mediaId $mediaId")
        if (!TextUtils.equals(mediaId, currentMediaId)) {
            currentMediaId = mediaId
            currentPosition = 0
        }
        val customData = JSONObject()
        customData.put(ITEM_ID, mediaId)
        val media = toCastMediaMetadata(track, customData)
        videoCastManager.loadMedia(media, autoPlay, currentPosition, customData)
    }

    private fun setMetadataFromRemote() {
        // Sync: We get the customData from the remote media information and update the local
        // metadata if it happens to be different from the one we are currently using.
        // This can happen when the app was either restarted/disconnected + connected, or if the
        // app joins an existing session while the Chromecast was playing a queue.
        try {
            val mediaInfo = videoCastManager.remoteMediaInformation ?: return
            val customData = mediaInfo.customData
            if (customData != null && customData.has(ITEM_ID)) {
                val remoteMediaId = customData.getString(ITEM_ID)
                if (!TextUtils.equals(currentMediaId, remoteMediaId)) {
                    currentMediaId = remoteMediaId
                    callback.setCurrentMediaId(remoteMediaId)
                    updateLastKnownStreamPosition()
                }
            }
        } catch (e: TransientNetworkDisconnectionException) {
            Timber.e(e, "Exception processing update metadata")
        } catch (e: NoConnectionException) {
            Timber.e(e, "Exception processing update metadata")
        } catch (e: JSONException) {
            Timber.e(e, "Exception processing update metadata")
        }
    }

    private fun updatePlaybackState() {
        val status = videoCastManager.playbackStatus
        val idleReason = videoCastManager.idleReason
        Timber.d("onRemoteMediaPlayerStatusUpdated %s", status)
        when (status) {
            MediaStatus.PLAYER_STATE_IDLE -> if (idleReason == MediaStatus.IDLE_REASON_FINISHED) {
                callback.onCompletion()
            }
            MediaStatus.PLAYER_STATE_BUFFERING -> {
                state = PlaybackStateCompat.STATE_BUFFERING
                callback.onPlaybackStatusChanged(state)
            }
            MediaStatus.PLAYER_STATE_PLAYING -> {
                state = PlaybackStateCompat.STATE_PLAYING
                setMetadataFromRemote()
                callback.onPlaybackStatusChanged(state)
            }
            MediaStatus.PLAYER_STATE_PAUSED -> {
                state = PlaybackStateCompat.STATE_PAUSED
                setMetadataFromRemote()
                callback.onPlaybackStatusChanged(state)
            }
            else -> Timber.d("State default : %s", status)
        }
    }

    companion object {
        private const val MIME_TYPE_AUDIO_MPEG = "audio/mpeg"
        private const val ITEM_ID = "itemId"

        /**
         * Helper method to convert a [android.media.MediaMetadata] to a
         * [com.google.android.gms.cast.MediaInfo] used for sending media to the receiver app.
         *
         * @param track [com.google.android.gms.cast.MediaMetadata]
         * @param customData custom data specifies the local mediaId used by the player.
         * @return mediaInfo [com.google.android.gms.cast.MediaInfo]
         */
        private fun toCastMediaMetadata(
            track: MediaMetadataCompat,
            customData: JSONObject
        ): MediaInfo {
            val mediaMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK)
            mediaMetadata.putString(
                MediaMetadata.KEY_TITLE,
                if (track.description.title == null) "" else track.description.title.toString()
            )
            mediaMetadata.putString(
                MediaMetadata.KEY_SUBTITLE,
                if (track.description.subtitle == null) "" else track.description.subtitle.toString()
            )
            mediaMetadata.putString(
                MediaMetadata.KEY_ALBUM_ARTIST,
                track.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST)
            )
            mediaMetadata.putString(
                MediaMetadata.KEY_ALBUM_TITLE,
                track.getString(MediaMetadataCompat.METADATA_KEY_ALBUM)
            )
            val image = WebImage(
                Uri.Builder().encodedPath(
                    track.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
                )
                    .build()
            )
            // First image is used by the receiver for showing the audio album art.
            mediaMetadata.addImage(image)
            // Second image is used by Cast Companion Library on the full screen activity that is shown
            // when the cast dialog is clicked.
            mediaMetadata.addImage(image)
            return MediaInfo.Builder(track.getString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE))
                .setContentType(MIME_TYPE_AUDIO_MPEG)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(mediaMetadata)
                .setCustomData(customData)
                .build()
        }
    }
}
