package never.ending.splendor.app.ui

import android.content.ComponentName
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.os.SystemClock
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.format.DateUtils
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.core.content.ContextCompat
import com.squareup.picasso.Picasso
import never.ending.splendor.R
import never.ending.splendor.app.MusicService
import never.ending.splendor.databinding.ActivityFullPlayerBinding
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.android.di
import org.kodein.di.instance
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * A full screen player that shows the current playing music with a background image
 * depicting the album art. The activity also has controls to seek/pause/play the audio.
 */
class FullScreenPlayerActivity : ActionBarCastActivity(), DIAware {

    override val di: DI by di()

    private val pauseDrawable: Drawable by lazy {
        requireNotNull(ContextCompat.getDrawable(this, R.drawable.uamp_ic_pause_white_48dp))
    }

    private val playDrawable: Drawable by lazy {
        requireNotNull(ContextCompat.getDrawable(this, R.drawable.uamp_ic_play_arrow_white_48dp))
    }

    private var mCurrentArtUrl: String? = null
    private val mHandler = Handler(Looper.getMainLooper())
    private var mMediaBrowser: MediaBrowserCompat? = null

    private val picasso: Picasso by instance()

    private val mUpdateProgressTask = Runnable { updateProgress() }
    private val mExecutorService = Executors.newSingleThreadScheduledExecutor()
    private var mScheduleFuture: ScheduledFuture<*>? = null
    private var mLastPlaybackState: PlaybackStateCompat? = null
    private val mCallback: MediaControllerCompat.Callback =
        object : MediaControllerCompat.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
                Timber.d("onPlaybackstate changed %s", state)
                updatePlaybackState(state)
            }

            override fun onMetadataChanged(metadata: MediaMetadataCompat) {
                val venue = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM)
                Timber.d("venue: %s", venue)
                val location = metadata.getString(MediaMetadataCompat.METADATA_KEY_AUTHOR)
                Timber.d("location: %s", location)
                updateMediaDescription(metadata.description, venue, location)
                updateDuration(metadata)
            }
        }
    private val mConnectionCallback: MediaBrowserCompat.ConnectionCallback =
        object : MediaBrowserCompat.ConnectionCallback() {
            override fun onConnected() {
                Timber.d("onConnected")
                try {
                    connectToSession(mMediaBrowser!!.sessionToken)
                } catch (e: RemoteException) {
                    Timber.e(e, "could not connect media controller")
                }
            }
        }

    private lateinit var binding: ActivityFullPlayerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")
        binding = ActivityFullPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeToolbar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        binding.next.setOnClickListener {
            val controls = supportMediaController!!.transportControls
            controls.skipToNext()
        }
        binding.prev.setOnClickListener {
            val controls = supportMediaController!!.transportControls
            controls.skipToPrevious()
        }

        binding.playPause.setOnClickListener {
            val state = supportMediaController!!.playbackState
            if (state != null) {
                val controls = supportMediaController!!.transportControls
                when (state.state) {
                    PlaybackStateCompat.STATE_PLAYING, PlaybackStateCompat.STATE_BUFFERING -> {
                        controls.pause()
                        stopSeekbarUpdate()
                    }
                    PlaybackStateCompat.STATE_PAUSED, PlaybackStateCompat.STATE_STOPPED -> {
                        controls.play()
                        scheduleSeekbarUpdate()
                    }
                    else -> Timber.d("onClick with state %s", state.state)
                }
            }
        }

        binding.seekBar1.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.startText.text = DateUtils.formatElapsedTime(progress / 1000.toLong())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                stopSeekbarUpdate()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                supportMediaController!!.transportControls.seekTo(seekBar.progress.toLong())
                scheduleSeekbarUpdate()
            }
        })
        mMediaBrowser = MediaBrowserCompat(
            this,
            ComponentName(this, MusicService::class.java), mConnectionCallback, null
        )
    }

    @Throws(RemoteException::class)
    private fun connectToSession(token: MediaSessionCompat.Token) {
        val mediaController = MediaControllerCompat(
            this@FullScreenPlayerActivity, token
        )
        if (mediaController.metadata == null) {
            finish()
            return
        }
        MediaControllerCompat.setMediaController(this, mediaController)
        mediaController.registerCallback(mCallback)
        val state = mediaController.playbackState
        updatePlaybackState(state)
        val metadata = mediaController.metadata
        if (metadata != null) {
            val venue = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM)
            Timber.d("venue: %s", venue)
            val location = metadata.getString(MediaMetadataCompat.METADATA_KEY_AUTHOR)
            Timber.d("location: %s", location)
            updateMediaDescription(metadata.description, venue, location)
            updateDuration(metadata)
        }
        updateProgress()
        if (state != null && (
            state.state == PlaybackStateCompat.STATE_PLAYING ||
                state.state == PlaybackStateCompat.STATE_BUFFERING
            )
        ) {
            scheduleSeekbarUpdate()
        }
    }

    private fun scheduleSeekbarUpdate() {
        stopSeekbarUpdate()
        if (!mExecutorService.isShutdown) {
            mScheduleFuture = mExecutorService.scheduleAtFixedRate(
                { mHandler.post(mUpdateProgressTask) }, PROGRESS_UPDATE_INITIAL_INTERVAL,
                PROGRESS_UPDATE_INTERNAL, TimeUnit.MILLISECONDS
            )
        }
    }

    private fun stopSeekbarUpdate() {
        if (mScheduleFuture != null) {
            mScheduleFuture!!.cancel(false)
        }
    }

    public override fun onStart() {
        super.onStart()
        if (mMediaBrowser != null) {
            mMediaBrowser!!.connect()
        }
    }

    public override fun onStop() {
        super.onStop()
        if (mMediaBrowser != null) {
            mMediaBrowser!!.disconnect()
        }
        if (supportMediaController != null) {
            supportMediaController!!.unregisterCallback(mCallback)
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        stopSeekbarUpdate()
        mExecutorService.shutdown()
    }

    private fun fetchImageAsync(description: MediaDescriptionCompat) {
        if (description.iconUri == null) {
            return
        }
        val artUrl = description.iconUri.toString()
        mCurrentArtUrl = artUrl
        picasso.load(artUrl)
            .fit()
            .centerInside()
            .into(binding.backgroundImage)
    }

    private fun updateMediaDescription(
        description: MediaDescriptionCompat?,
        venue: String,
        location: String
    ) {
        if (description == null) {
            return
        }
        Timber.d("updateMediaDescription called ")
        binding.line1.text = description.title
        binding.line2.text = description.description
        binding.line3.text = venue
        binding.line4.text = location
        fetchImageAsync(description)
    }

    private fun updateDuration(metadata: MediaMetadataCompat?) {
        if (metadata == null) {
            return
        }
        Timber.d("updateDuration called ")
        val duration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION).toInt()
        binding.seekBar1.max = duration
        binding.endText.text = DateUtils.formatElapsedTime(duration / 1000.toLong())
    }

    private fun updatePlaybackState(state: PlaybackStateCompat?) {
        if (state == null) {
            return
        }
        mLastPlaybackState = state
        if (supportMediaController != null && supportMediaController!!.extras != null) {
            val castName = supportMediaController!!
                .extras.getString(MusicService.EXTRA_CONNECTED_CAST)
            val line3Text = if (castName == null) "" else resources
                .getString(R.string.casting_to_device, castName)
            binding.line5.text = line3Text
        }
        when (state.state) {
            PlaybackStateCompat.STATE_PLAYING -> {
                binding.progressBar1.visibility = View.INVISIBLE
                binding.playPause.visibility = View.VISIBLE
                binding.playPause.setImageDrawable(pauseDrawable)
                binding.controllers.visibility = View.VISIBLE
                scheduleSeekbarUpdate()
            }
            PlaybackStateCompat.STATE_PAUSED -> {
                binding.controllers.visibility = View.VISIBLE
                binding.progressBar1.visibility = View.INVISIBLE
                binding.playPause.visibility = View.VISIBLE
                binding.playPause.setImageDrawable(playDrawable)
                stopSeekbarUpdate()
            }
            PlaybackStateCompat.STATE_NONE, PlaybackStateCompat.STATE_STOPPED -> {
                binding.progressBar1.visibility = View.INVISIBLE
                binding.playPause.visibility = View.VISIBLE
                binding.playPause.setImageDrawable(playDrawable)
                stopSeekbarUpdate()
            }
            PlaybackStateCompat.STATE_BUFFERING -> {
                binding.playPause.visibility = View.INVISIBLE
                binding.progressBar1.visibility = View.VISIBLE
                binding.line5.setText(R.string.loading)
                stopSeekbarUpdate()
            }
            else -> Timber.d("Unhandled state %s", state.state)
        }
        binding.next.visibility =
            if (state.actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT == 0L) View.INVISIBLE else View.VISIBLE
        binding.next.visibility =
            if (state.actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS == 0L) View.INVISIBLE else View.VISIBLE
    }

    private fun updateProgress() {
        if (mLastPlaybackState == null) {
            return
        }
        var currentPosition = mLastPlaybackState!!.position
        if (mLastPlaybackState!!.state != PlaybackStateCompat.STATE_PAUSED) {
            // Calculate the elapsed time between the last position update and now and unless
            // paused, we can assume (delta * speed) + current position is approximately the
            // latest position. This ensure that we do not repeatedly call the getPlaybackState()
            // on MediaControllerCompat.
            val timeDelta = SystemClock.elapsedRealtime() -
                mLastPlaybackState!!.lastPositionUpdateTime
            currentPosition += timeDelta.toInt() * mLastPlaybackState!!.playbackSpeed.toLong()
        }
        binding.seekBar1.progress = currentPosition.toInt()
    }

    private val supportMediaController: MediaControllerCompat?
        get() = MediaControllerCompat.getMediaController(this)

    companion object {
        private const val PROGRESS_UPDATE_INTERNAL: Long = 1000
        private const val PROGRESS_UPDATE_INITIAL_INTERVAL: Long = 100
    }
}
