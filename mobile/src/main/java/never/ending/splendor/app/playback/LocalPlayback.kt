package never.ending.splendor.app.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.MediaPlayer
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.WifiLock
import android.os.Build
import android.os.PowerManager
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import never.ending.splendor.app.MusicService
import never.ending.splendor.app.model.MusicProvider
import never.ending.splendor.app.model.MusicProviderSource
import never.ending.splendor.app.utils.MediaIdHelper.musicId
import timber.log.Timber
import java.io.IOException

/**
 * A class that implements local media playback using [android.media.MediaPlayer]
 */
class LocalPlayback(
    private val context: Context,
    private val musicProvider: MusicProvider
) : Playback,
    OnAudioFocusChangeListener,
    MediaPlayer.OnCompletionListener,
    MediaPlayer.OnErrorListener,
    MediaPlayer.OnPreparedListener,
    MediaPlayer.OnSeekCompleteListener {

    override var state: Int = PlaybackStateCompat.STATE_NONE

    private var playOnFocusGain = false

    override var callback: Playback.Callback = Playback.Callback.EMPTY

    @Volatile
    private var audioNoisyReceiverRegistered = false

    @Volatile
    private var currentPosition = 0

    @Volatile
    override var currentMediaId: String? = null

    // Type of audio focus we have:
    private var audioFocus = AUDIO_NO_FOCUS_NO_DUCK
    private lateinit var mediaPlayerA: MediaPlayer
    private lateinit var mediaPlayerB: MediaPlayer
    private val mediaPlayer get() = requireNotNull(_mediaPlayer)
    private var _mediaPlayer: MediaPlayer? = null
    private var mediaPlayersSwapping = false

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val wifiLock: WifiLock = (
        context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        )
        .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "uAmp_lock")

    @Volatile
    private var mNextMediaId: String? = null

    override val supportsGapless: Boolean = true

    private fun nextMediaPlayer(): MediaPlayer {
        Timber.i("nextMediaPlayer() currentPlayer=%s", mediaPlayer)
        return if (mediaPlayer === mediaPlayerA) mediaPlayerB else mediaPlayerA
    }

    private val mAudioNoisyIntentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private val mAudioNoisyReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
                Timber.i("Headphones disconnected.")
                if (isPlaying) {
                    val i = Intent(context, MusicService::class.java)
                    i.action = MusicService.ACTION_CMD
                    i.putExtra(MusicService.CMD_NAME, MusicService.CMD_PAUSE)
                    this@LocalPlayback.context.startService(i)
                }
            }
        }
    }

    override fun start() = Unit

    override fun stop(notifyListeners: Boolean) {
        Timber.i("stop notifyListeners=%s", notifyListeners)
        state = PlaybackStateCompat.STATE_STOPPED

        if (notifyListeners) {
            callback.onPlaybackStatusChanged(state)
        }

        currentPosition = currentStreamPosition
        // Give up Audio focus
        giveUpAudioFocus()
        unregisterAudioNoisyReceiver()
        // Relax all resources
        relaxResources(true)
    }

    override val isConnected: Boolean = true
    override val isPlaying: Boolean get() = playOnFocusGain || _mediaPlayer != null && mediaPlayer.isPlaying

    override var currentStreamPosition: Int = 0
        get() = if (_mediaPlayer != null) mediaPlayer.currentPosition else field

    override fun updateLastKnownStreamPosition() {
        currentPosition = if(_mediaPlayer != null) mediaPlayer.currentPosition else 0
    }

    override fun playNext(item: MediaSessionCompat.QueueItem): Boolean {
        Timber.i("playNext() item=%s", item)
        val nextPlayer: MediaPlayer =
            if (mediaPlayer === mediaPlayerA) mediaPlayerB
            else mediaPlayerA

        val mediaId = item.description.mediaId
        val mediaHasChanged = mediaId != currentMediaId
        if (mediaHasChanged) {
            mNextMediaId = mediaId
        }
        val track = musicProvider.getMusic(
            item.description.mediaId?.musicId
        )
        val source = track!!.getString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE)
        nextPlayer.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        )
        try {
            nextPlayer.setDataSource(source)
        } catch (ex: IOException) {
            Timber.e(ex, "Exception playing song")
            callback.onError(ex.message)
        }

        // Starts preparing the media player in the background. When
        // it's done, it will call our OnPreparedListener (that is,
        // the onPrepared() method on this class, since we set the
        // listener to 'this'). Until the media player is prepared,
        // we *cannot* call start() on it!
        nextPlayer.prepareAsync()
        mediaPlayersSwapping = true
        return true
    }

    override fun play(item: MediaSessionCompat.QueueItem) {
        Timber.i("play() item=%s", item)

        // we never call this if we're auto-queued due to gapless
        if (mediaPlayersSwapping) {
            mediaPlayersSwapping = false
        }
        playOnFocusGain = true
        tryToGetAudioFocus()
        registerAudioNoisyReceiver()
        val mediaId = item.description.mediaId
        val mediaHasChanged = mediaId != currentMediaId
        if (mediaHasChanged) {
            currentPosition = 0
            currentMediaId = mediaId
        }
        if (state == PlaybackStateCompat.STATE_PAUSED && !mediaHasChanged) {
            configMediaPlayerState()
        } else {
            state = PlaybackStateCompat.STATE_STOPPED
            relaxResources(false) // release everything except MediaPlayer
            val track = musicProvider.getMusic(
                item.description.mediaId?.musicId
            )
            val source = track!!.getString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE)
            try {
                createMediaPlayerIfNeeded()
                state = PlaybackStateCompat.STATE_BUFFERING
                mediaPlayer.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                mediaPlayer.setDataSource(source)

                // Starts preparing the media player in the background. When
                // it's done, it will call our OnPreparedListener (that is,
                // the onPrepared() method on this class, since we set the
                // listener to 'this'). Until the media player is prepared,
                // we *cannot* call start() on it!
                mediaPlayer.prepareAsync()

                // If we are streaming from the internet, we want to hold a
                // Wifi lock, which prevents the Wifi radio from going to
                // sleep while the song is playing.
                wifiLock.acquire()
                callback.onPlaybackStatusChanged(state)
            } catch (ex: IOException) {
                Timber.e(ex, "Exception playing song")
                callback.onError(ex.message)
            }
        }
    }

    override fun pause() {
        Timber.i("pause()")
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            // Pause media player and cancel the 'foreground service' state.
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                currentPosition = mediaPlayer.currentPosition
            }
            // while paused, retain the MediaPlayer but give up audio focus
            relaxResources(false)
            giveUpAudioFocus()
        }
        state = PlaybackStateCompat.STATE_PAUSED
        callback.onPlaybackStatusChanged(state)
        unregisterAudioNoisyReceiver()
    }

    override fun seekTo(position: Int) {
        Timber.i("seekTo called with %s", position)
        if (mediaPlayer.isPlaying) {
            state = PlaybackStateCompat.STATE_BUFFERING
        }
        mediaPlayer.seekTo(position)
        callback.onPlaybackStatusChanged(state)
    }

    /**
     * Try to get the system audio focus.
     */
    private fun tryToGetAudioFocus() {
        Timber.i("tryToGetAudioFocus")
        if (audioFocus != AUDIO_FOCUSED) {
            @Suppress("DEPRECATION") // new way to do this isn't availalble until api 26.
            val result = audioManager.requestAudioFocus(
                this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                audioFocus = AUDIO_FOCUSED
            }
        }
    }

    /**
     * Give up the audio focus.
     */
    private fun giveUpAudioFocus() {
        Timber.i("giveUpAudioFocus")
        if (audioFocus == AUDIO_FOCUSED) {
            @Suppress("DEPRECATION") // new api not available until api 26
            if (audioManager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                audioFocus = AUDIO_NO_FOCUS_NO_DUCK
            }
        }
    }

    /**
     * Reconfigures MediaPlayer according to audio focus settings and
     * starts/restarts it. This method starts/restarts the MediaPlayer
     * respecting the current audio focus state. So if we have focus, it will
     * play normally; if we don't have focus, it will either leave the
     * MediaPlayer paused or set it to a low volume, depending on what is
     * allowed by the current focus settings. This method assumes mPlayer !=
     * null, so if you are calling it, you have to do so from a context where
     * you are sure this is the case.
     */
    private fun configMediaPlayerState() {
        Timber.i("configMediaPlayerState. mAudioFocus=%s", audioFocus)
        if (audioFocus == AUDIO_NO_FOCUS_NO_DUCK) {
            // If we don't have audio focus and can't duck, we have to pause,
            if (state == PlaybackStateCompat.STATE_PLAYING) {
                pause()
            }
        } else { // we have audio focus:
            if (audioFocus == AUDIO_NO_FOCUS_CAN_DUCK) {
                mediaPlayer.setVolume(VOLUME_DUCK, VOLUME_DUCK) // we'll be relatively quiet
            } else {
                mediaPlayer.setVolume(VOLUME_NORMAL, VOLUME_NORMAL) // we can be loud again
            }
            // If we were playing when we lost focus, we need to resume playing.
            if (playOnFocusGain) {
                if (!mediaPlayer.isPlaying) {
                    Timber.i(
                        "configMediaPlayerState startMediaPlayer. seeking to %s ",
                        currentPosition
                    )
                    state = if (currentPosition == mediaPlayer.currentPosition) {
                        mediaPlayer.start()
                        PlaybackStateCompat.STATE_PLAYING
                    } else {
                        mediaPlayer.seekTo(currentPosition)
                        PlaybackStateCompat.STATE_BUFFERING
                    }
                }
                playOnFocusGain = false
            }
        }
        callback.onPlaybackStatusChanged(state)
    }

    /**
     * Called by AudioManager on audio focus changes.
     * Implementation of [android.media.AudioManager.OnAudioFocusChangeListener]
     */
    override fun onAudioFocusChange(focusChange: Int) {
        Timber.i("onAudioFocusChange. focusChange=%s", focusChange)
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            // We have gained focus:
            audioFocus = AUDIO_FOCUSED
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            // We have lost focus. If we can duck (low playback volume), we can keep playing.
            // Otherwise, we need to pause the playback.
            val canDuck = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
            audioFocus = if (canDuck) AUDIO_NO_FOCUS_CAN_DUCK else AUDIO_NO_FOCUS_NO_DUCK

            // If we are playing, we need to reset media player by calling configMediaPlayerState
            // with mAudioFocus properly set.
            if (state == PlaybackStateCompat.STATE_PLAYING && !canDuck) {
                // If we don't have audio focus and can't duck, we save the information that
                // we were playing, so that we can resume playback once we get the focus back.
                playOnFocusGain = true
            }
        } else {
            Timber.e("onAudioFocusChange: Ignoring unsupported focusChange: %s", focusChange)
        }
        configMediaPlayerState()
    }

    /**
     * Called when MediaPlayer has completed a seek
     */
    override fun onSeekComplete(mp: MediaPlayer) {
        Timber.i("onSeekComplete from MediaPlayer: %s", mp.currentPosition)
        currentPosition = mp.currentPosition
        if (state == PlaybackStateCompat.STATE_BUFFERING) {
            mediaPlayer.start()
            state = PlaybackStateCompat.STATE_PLAYING
        }
        callback.onPlaybackStatusChanged(state)
    }

    /**
     * Called when media player is done playing current song.
     */
    override fun onCompletion(player: MediaPlayer) {
        Timber.i("onCompletion from MediaPlayer")
        // The media player finished playing the current song, so we go ahead
        // and start the next.
        if (mediaPlayersSwapping) {
            currentPosition = 0
            currentMediaId = mNextMediaId
            val old = mediaPlayer
            _mediaPlayer = nextMediaPlayer() // we're now using the new media player
            mediaPlayersSwapping = false
            old.reset() // required for the next time we swap
            callback.onPlaybackStatusChanged(state)
        }
        callback.onCompletion()
    }

    /**
     * Called when media player is done preparing.
     */
    override fun onPrepared(player: MediaPlayer) {
        Timber.i("onPrepared() player=%s", player)
        if (mediaPlayersSwapping) {
            // when the next player is prepared, go ahead and set it as next
            mediaPlayer.setNextMediaPlayer(nextMediaPlayer())
            return
        }

        // The media player is done preparing. That means we can start playing if we
        // have audio focus.
        configMediaPlayerState()
    }

    /**
     * Called when there's an error playing media. When this happens, the media
     * player goes to the Error state. We warn the user about the error and
     * reset the media player
     */
    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        val logError = """"Media player error: what=%s extra=%s"
            TrackInfo=%s
            DrmInfo=%s
            Metrics=%s
            MediaPlayer=%s
        """.trimIndent()
        Timber.e(
            logError, what, extra, runCatching { mp.trackInfo }.getOrNull(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) runCatching { mp.drmInfo } else "",
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) runCatching { mp.metrics } else "",
            mp
        )
        callback.onError("MediaPlayer error $what ($extra)")
        mp.reset()
        return true
    }

    private fun createMediaPlayerIfNeeded() {
        mediaPlayerA = createMediaPlayer(if (this::mediaPlayerA.isInitialized) mediaPlayerA else null)
        mediaPlayerB = createMediaPlayer(if (this::mediaPlayerB.isInitialized) mediaPlayerB else null)
        if (_mediaPlayer == null) _mediaPlayer = mediaPlayerA
    }

    /**
     * Makes sure the media player exists and has been reset. This will create
     * the media player if needed, or reset the existing media player if one
     * already exists.
     */
    private fun createMediaPlayer(player: MediaPlayer?): MediaPlayer {
        var nextPlayer = player
        Timber.i("createMediaPlayerIfNeeded. needed? %s", nextPlayer == null)
        if (nextPlayer == null) {
            nextPlayer = MediaPlayer()

            // Make sure the media player will acquire a wake-lock while
            // playing. If we don't do that, the CPU might go to sleep while the
            // song is playing, causing playback to stop.
            nextPlayer.setWakeMode(
                context.applicationContext,
                PowerManager.PARTIAL_WAKE_LOCK
            )

            // we want the media player to notify us when it's ready preparing,
            // and when it's done playing:
            nextPlayer.setOnPreparedListener(this)
            nextPlayer.setOnCompletionListener(this)
            nextPlayer.setOnErrorListener(this)
            nextPlayer.setOnSeekCompleteListener(this)
        } else {
            nextPlayer.reset()
        }
        return nextPlayer
    }

    /**
     * Releases resources used by the service for playback. This includes the
     * "foreground service" status, the wake locks and possibly the MediaPlayer.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also
     * be released or not
     */
    private fun relaxResources(releaseMediaPlayer: Boolean) {
        Timber.i("relaxResources. releaseMediaPlayer=%s", releaseMediaPlayer)

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer) {
            _mediaPlayer?.reset()
            _mediaPlayer?.release()
            _mediaPlayer = null
        }

        // we can also release the Wifi lock, if we're holding it
        if (wifiLock.isHeld) {
            wifiLock.release()
        }
    }

    private fun registerAudioNoisyReceiver() {
        if (!audioNoisyReceiverRegistered) {
            context.registerReceiver(mAudioNoisyReceiver, mAudioNoisyIntentFilter)
            audioNoisyReceiverRegistered = true
        }
    }

    private fun unregisterAudioNoisyReceiver() {
        if (audioNoisyReceiverRegistered) {
            context.unregisterReceiver(mAudioNoisyReceiver)
            audioNoisyReceiverRegistered = false
        }
    }

    companion object {
        // The volume we set the media player to when we lose audio focus, but are
        // allowed to reduce the volume instead of stopping playback.
        const val VOLUME_DUCK = 0.2f

        // The volume we set the media player when we have audio focus.
        const val VOLUME_NORMAL = 1.0f

        // we don't have audio focus, and can't duck (play at a low volume)
        private const val AUDIO_NO_FOCUS_NO_DUCK = 0

        // we don't have focus, but can duck (play at a low volume)
        private const val AUDIO_NO_FOCUS_CAN_DUCK = 1

        // we have full audio focus
        private const val AUDIO_FOCUSED = 2
    }
}
