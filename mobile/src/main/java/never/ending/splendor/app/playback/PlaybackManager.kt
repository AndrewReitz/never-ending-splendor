package never.ending.splendor.app.playback

import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import never.ending.splendor.R
import never.ending.splendor.app.model.MusicProvider
import never.ending.splendor.app.utils.MediaIdHelper.musicId
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Manage the interactions among the container service, the queue manager and the actual playback.
 */
class PlaybackManager(
    private val mServiceCallback: PlaybackServiceCallback,
    private val mResources: Resources,
    private val mMusicProvider: MusicProvider,
    private val mQueueManager: QueueManager,
    playback: Playback?
) : Playback.Callback {

    var playback: Playback?
        private set

    private val mMediaSessionCallback: MediaSessionCallback
    private val mScheduleFuture: ScheduledFuture<*>
    private val mExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val mHandler = Handler(Looper.getMainLooper())
    private var mGaplessQueued = false
    val mediaSessionCallback: MediaSessionCompat.Callback
        get() = mMediaSessionCallback
    private val mMonitorPositionTask = Runnable { monitorPosition() }
    private fun monitorPosition() {
        if (playback == null) {
            return
        }
        if (playback!!.supportsGapless && playback!!.isPlaying && !mGaplessQueued) {
            val currentPosition = playback!!.currentStreamPosition / 1000.toLong()
            val duration = mQueueManager.duration / 1000
            val delta = duration - currentPosition
            Timber.d("delta: %s", delta)
            if (duration - currentPosition == QUEUE_NEXT_TRACK_TIME) {
                if (mQueueManager.skipQueuePosition(1)) {
                    val currentMusic = mQueueManager.currentMusic
                    if (currentMusic != null) {
                        // mServiceCallback.onPlaybackStart();
                        Timber.d("Queing up next track : %s", currentMusic.description.title)
                        playback!!.playNext(currentMusic)
                        mGaplessQueued = true
                    }
                } else {
                    handleStopRequest("Cannot skip")
                }
            }
        }
    }

    /**
     * Handle a request to play music
     */
    fun handlePlayRequest() {
        mGaplessQueued = false // this was a request from user.  mPlayback.play will cancel gapless
        Timber.d("handlePlayRequest: mState=%s", playback!!.state)
        val currentMusic = mQueueManager.currentMusic
        if (currentMusic != null) {
            mServiceCallback.onPlaybackStart()
            playback!!.play(currentMusic)
        }
    }

    /**
     * Handle a request to pause music
     */
    fun handlePauseRequest() {
        Timber.d("handlePauseRequest: mState=%s", playback!!.state)
        if (playback!!.isPlaying) {
            playback!!.pause()
            mServiceCallback.onPlaybackStop()
        }
    }

    /**
     * Handle a request to stop music
     *
     * @param withError Error message in case the stop has an unexpected cause. The error
     * message will be set in the PlaybackState and will be visible to
     * MediaController clients.
     */
    fun handleStopRequest(withError: String?) {
        Timber.d("handleStopRequest: mState=%s error=%s", playback!!.state, withError)
        playback!!.stop(true)
        mServiceCallback.onPlaybackStop()
        updatePlaybackState(withError)
    }

    /**
     * Update the current media player state, optionally showing an error message.
     *
     * @param error if not null, error message to present to the user.
     */
    fun updatePlaybackState(error: String?) {
        Timber.d("updatePlaybackState, playback state=%s", playback!!.state)
        var position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN
        if (playback != null && playback!!.isConnected) {
            position = playback!!.currentStreamPosition.toLong()
        }
        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(availableActions)
        setCustomAction(stateBuilder)
        var state = playback!!.state

        // If there is an error message, send it to the playback state:
        if (error != null) {
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            stateBuilder.setErrorMessage(PlaybackStateCompat.ERROR_CODE_UNKNOWN_ERROR, error)
            state = PlaybackStateCompat.STATE_ERROR
        }
        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime())

        // Set the activeQueueItemId if the current index is valid.
        val currentMusic = mQueueManager.currentMusic
        if (currentMusic != null) {
            stateBuilder.setActiveQueueItemId(currentMusic.queueId)
        }
        mServiceCallback.onPlaybackStateUpdated(stateBuilder.build())
        if (state == PlaybackStateCompat.STATE_PLAYING ||
            state == PlaybackStateCompat.STATE_PAUSED
        ) {
            mServiceCallback.onNotificationRequired()
        }
    }

    private fun setCustomAction(stateBuilder: PlaybackStateCompat.Builder) {
        val currentMusic = mQueueManager.currentMusic ?: return
        // Set appropriate "Favorite" icon on Custom action:
        val mediaId = currentMusic.description.mediaId ?: return
        val musicId = mediaId.musicId
        val favoriteIcon =
            if (mMusicProvider.isFavorite(musicId)) R.drawable.ic_star_on else R.drawable.ic_star_off
        Timber.d(
            "updatePlaybackState, setting Favorite custom action of music %s current favorite=%s",
            musicId, mMusicProvider.isFavorite(musicId)
        )
        val customActionExtras = Bundle()
        stateBuilder.addCustomAction(
            PlaybackStateCompat.CustomAction.Builder(
                CUSTOM_ACTION_THUMBS_UP, mResources.getString(R.string.favorite), favoriteIcon
            )
                .setExtras(customActionExtras)
                .build()
        )
    }

    private val availableActions: Long
        get() {
            var actions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            if (playback!!.isPlaying) {
                actions = actions or PlaybackStateCompat.ACTION_PAUSE
            }
            return actions
        }

    /**
     * Implementation of the Playback.Callback interface
     */
    override fun onCompletion() {
        // The media player finished playing the current song, so we go ahead
        // and start the next.
        when {
            mGaplessQueued -> {
                mServiceCallback.onPlaybackStart()
                mQueueManager.updateMetadata()
                mGaplessQueued = false
            }
            mQueueManager.skipQueuePosition(1) -> {
                handlePlayRequest()
                mQueueManager.updateMetadata()
            }
            else -> {
                // If skipping was not possible, we stop and release the resources:
                handleStopRequest(null)
            }
        }
    }

    override fun onPlaybackStatusChanged(state: Int) {
        updatePlaybackState(null)
    }

    override fun onError(error: String?) {
        updatePlaybackState(error)
    }

    override fun setCurrentMediaId(mediaId: String?) {
        Timber.d("setCurrentMediaId %s", mediaId)
        mQueueManager.setQueueFromMusic(mediaId!!)
    }

    /**
     * Switch to a different Playback instance, maintaining all playback state, if possible.
     *
     * @param playback switch to this playback
     */
    fun switchToPlayback(playback: Playback?, resumePlaying: Boolean) {
        requireNotNull(playback) { "Playback cannot be null" }
        // suspend the current one.
        val oldState = playback.state
        val pos = playback.currentStreamPosition
        val currentMediaId = playback.currentMediaId
        playback.stop(false)
        playback.callback = this
        playback.currentStreamPosition = if (pos < 0) 0 else pos
        playback.currentMediaId = currentMediaId
        playback.start()
        // finally swap the instance
        this.playback = playback
        when (oldState) {
            PlaybackStateCompat.STATE_BUFFERING, PlaybackStateCompat.STATE_CONNECTING, PlaybackStateCompat.STATE_PAUSED -> playback.pause()
            PlaybackStateCompat.STATE_PLAYING -> {
                val currentMusic = mQueueManager.currentMusic
                if (resumePlaying && currentMusic != null) {
                    playback.play(currentMusic)
                } else if (!resumePlaying) {
                    playback.pause()
                } else {
                    playback.stop(true)
                }
            }
            PlaybackStateCompat.STATE_NONE -> {
            }
            else -> Timber.d("Default called. Old state is %s", oldState)
        }
    }

    private inner class MediaSessionCallback : MediaSessionCompat.Callback() {
        override fun onPlay() {
            Timber.d("play")
            if (mQueueManager.currentMusic == null) {
                mQueueManager.setRandomQueue()
            }
            handlePlayRequest()
        }

        override fun onSkipToQueueItem(queueId: Long) {
            Timber.d("OnSkipToQueueItem: %s", queueId)
            mQueueManager.setCurrentQueueItem(queueId)
            handlePlayRequest()
            mQueueManager.updateMetadata()
        }

        override fun onSeekTo(position: Long) {
            Timber.d("onSeekTo: %s", position)
            playback!!.seekTo(position.toInt())
        }

        override fun onPlayFromMediaId(mediaId: String, extras: Bundle) {
            Timber.d("playFromMediaId mediaId: %s extras=%s", mediaId, extras)
            mQueueManager.setQueueFromMusic(mediaId)
            handlePlayRequest()
        }

        override fun onPause() {
            Timber.d("pause. current state=%s", playback!!.state)
            handlePauseRequest()
        }

        override fun onStop() {
            Timber.d("stop. current state=%s", playback!!.state)
            handleStopRequest(null)
        }

        override fun onSkipToNext() {
            Timber.d("skipToNext")
            if (mQueueManager.skipQueuePosition(1)) {
                handlePlayRequest()
            } else {
                handleStopRequest("Cannot skip")
            }
            mQueueManager.updateMetadata()
        }

        override fun onSkipToPrevious() {
            if (mQueueManager.skipQueuePosition(-1)) {
                handlePlayRequest()
            } else {
                handleStopRequest("Cannot skip")
            }
            mQueueManager.updateMetadata()
        }

        override fun onCustomAction(action: String, extras: Bundle) {
            if (CUSTOM_ACTION_THUMBS_UP == action) {
                Timber.i("onCustomAction: favorite for current track")
                val currentMusic = mQueueManager.currentMusic
                if (currentMusic != null) {
                    val mediaId = currentMusic.description.mediaId
                    if (mediaId != null) {
                        val musicId = mediaId.musicId
                        mMusicProvider.setFavorite(musicId!!, !mMusicProvider.isFavorite(musicId))
                    }
                }
                // playback state needs to be updated because the "Favorite" icon on the
                // custom action will change to reflect the new favorite state.
                updatePlaybackState(null)
            } else {
                Timber.e("Unsupported action: %s", action)
            }
        }

        /**
         * Handle free and contextual searches.
         *
         *
         * All voice searches on Android Auto are sent to this method through a connected
         * [android.support.v4.media.session.MediaControllerCompat].
         *
         *
         * Threads and async handling:
         * Search, as a potentially slow operation, should run in another thread.
         *
         *
         * Since this method runs on the main thread, most apps with non-trivial metadata
         * should defer the actual search to another thread (for example, by using
         * an [AsyncTask] as we do here).
         */
        override fun onPlayFromSearch(query: String, extras: Bundle) {
            Timber.d("playFromSearch  query=%s extras=%s", query, extras)
            playback!!.state = PlaybackStateCompat.STATE_CONNECTING
            val successSearch = mQueueManager.setQueueFromSearch(query, extras)
            if (successSearch) {
                handlePlayRequest()
                mQueueManager.updateMetadata()
            } else {
                updatePlaybackState("Could not find music")
            }
        }
    }

    interface PlaybackServiceCallback {
        fun onPlaybackStart()
        fun onNotificationRequired()
        fun onPlaybackStop()
        fun onPlaybackStateUpdated(newState: PlaybackStateCompat)
    }

    companion object {
        // Action to thumbs up a media item
        private const val CUSTOM_ACTION_THUMBS_UP = "com.example.android.uamp.THUMBS_UP"
        private const val QUEUE_NEXT_TRACK_TIME: Long =
            10 // queue next track N seconds before this one ends
        private const val PROGRESS_UPDATE_INTERNAL: Long = 1000
        private const val PROGRESS_UPDATE_INITIAL_INTERVAL: Long = 100
    }

    init {
        mMediaSessionCallback = MediaSessionCallback()
        this.playback = playback
        playback!!.callback = this
        mScheduleFuture = mExecutorService.scheduleAtFixedRate(
            { mHandler.post(mMonitorPositionTask) }, PROGRESS_UPDATE_INITIAL_INTERVAL,
            PROGRESS_UPDATE_INTERNAL, TimeUnit.MILLISECONDS
        )
    }
}
