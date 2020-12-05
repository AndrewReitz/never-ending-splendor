package never.ending.splendor.app

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.ApplicationMetadata
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import never.ending.splendor.R
import never.ending.splendor.app.model.MusicProvider
import never.ending.splendor.app.playback.CastPlayback
import never.ending.splendor.app.playback.LocalPlayback
import never.ending.splendor.app.playback.Playback
import never.ending.splendor.app.playback.PlaybackManager
import never.ending.splendor.app.playback.PlaybackManager.PlaybackServiceCallback
import never.ending.splendor.app.playback.QueueManager
import never.ending.splendor.app.playback.QueueManager.MetadataUpdateListener
import never.ending.splendor.app.ui.MusicPlayerActivity
import never.ending.splendor.app.utils.CarHelper
import never.ending.splendor.app.utils.MediaIdHelper
import org.kodein.di.DIAware
import org.kodein.di.android.di
import org.kodein.di.instance
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * This class provides a MediaBrowser through a service. It exposes the media library to a browsing
 * client, through the onGetRoot and onLoadChildren methods. It also creates a MediaSession and
 * exposes it through its MediaSession.Token, which allows the client to create a MediaController
 * that connects to and send control commands to the MediaSession remotely. This is useful for
 * user interfaces that need to interact with your media session, like Android Auto. You can
 * (should) also use the same service from your app's UI, which gives a seamless playback
 * experience to the user.
 *
 * To implement a MediaBrowserService, you need to:
 *
 *
 *
 *  *  Extend [android.service.media.MediaBrowserService], implementing the media browsing
 * related methods [android.service.media.MediaBrowserService.onGetRoot] and
 * [android.service.media.MediaBrowserService.onLoadChildren];
 *  *  In onCreate, start a new [android.media.session.MediaSession] and notify its parent
 * with the session's token [android.service.media.MediaBrowserService.setSessionToken];
 *
 *  *  Set a callback on the
 * [android.media.session.MediaSession.setCallback].
 * The callback will receive all the user's actions, like play, pause, etc;
 *
 *  *  Handle all the actual music playing using any method your app prefers (for example,
 * [android.media.MediaPlayer])
 *
 *  *  Update playbackState, "now playing" metadata and queue, using MediaSession proper methods
 * [android.media.session.MediaSession.setPlaybackState]
 * [android.media.session.MediaSession.setMetadata] and
 * [android.media.session.MediaSession.setQueue])
 *
 *  *  Declare and export the service in AndroidManifest with an intent receiver for the action
 * android.media.browse.MediaBrowserService
 *
 *
 *
 * To make your app compatible with Android Auto, you also need to:
 *
 *
 *
 *  *  Declare a meta-data tag in AndroidManifest.xml linking to a xml resource
 * with a &lt;automotiveApp&gt; root element. For a media app, this must include
 * an &lt;uses name="media"/&gt; element as a child.
 * For example, in AndroidManifest.xml:
 * &lt;meta-data android:name="com.google.android.gms.car.application"
 * android:resource="@xml/automotive_app_desc"/&gt;
 * And in res/values/automotive_app_desc.xml:
 * &lt;automotiveApp&gt;
 * &lt;uses name="media"/&gt;
 * &lt;/automotiveApp&gt;
 *
 *
 *
 * @see [README.md](README.md) for more details.
 */
// todo fix main scope to be part of the lifecycle (kill in ondestroy)
class MusicService : MediaBrowserServiceCompat(), PlaybackServiceCallback, DIAware, CoroutineScope by MainScope() {

    override val di by di()

    private var mPlaybackManager: PlaybackManager? = null
    private var mSession: MediaSessionCompat? = null
    private var mSessionExtras: Bundle? = null
    private val mDelayedStopHandler = DelayedStopHandler(this)
    private var mMediaRouter: MediaRouter? = null

    // car stuff not actually sure if this is needed
    private var isConnectedToCar = false
    private var carConnectionReceiver: BroadcastReceiver? = null

    private val videoCastManager: VideoCastManager by instance()
    private val musicProvider: MusicProvider by instance()
    private val picasso: Picasso by instance()
    private val notificationManager: NotificationManagerCompat by instance()

    // todo move to DI
    private val mediaNotificationManager: MediaNotificationManager by lazy {
        MediaNotificationManager(this, picasso, notificationManager)
    }

    /**
     * Consumer responsible for switching the Playback instances depending on whether
     * it is connected to a remote player.
     *
     * TODO move to it's own class
     */
    private val mCastConsumer: VideoCastConsumerImpl = object : VideoCastConsumerImpl() {
        override fun onApplicationConnected(
            appMetadata: ApplicationMetadata,
            sessionId: String,
            wasLaunched: Boolean
        ) { // In case we are casting, send the device name as an extra on MediaSession metadata.
            mSessionExtras!!.putString(EXTRA_CONNECTED_CAST, videoCastManager.deviceName)
            mSession!!.setExtras(mSessionExtras)
            // Now we can switch to CastPlayback
            val playback: Playback = CastPlayback(musicProvider, videoCastManager)
            mMediaRouter!!.setMediaSessionCompat(mSession)
            mPlaybackManager!!.switchToPlayback(playback, true)
        }

        override fun onDisconnectionReason(reason: Int) {
            Timber.d("onDisconnectionReason")
            // This is our final chance to update the underlying stream position
            // In onDisconnected(), the underlying CastPlayback#mVideoCastConsumer
            // is disconnected and hence we update our local value of stream position
            // to the latest position.
            mPlaybackManager!!.playback?.updateLastKnownStreamPosition()
        }

        override fun onDisconnected() {
            Timber.d("onDisconnected")
            mSessionExtras!!.remove(EXTRA_CONNECTED_CAST)
            mSession!!.setExtras(mSessionExtras)
            val playback: Playback = LocalPlayback(this@MusicService, musicProvider)
            mMediaRouter!!.setMediaSessionCompat(null)
            mPlaybackManager!!.switchToPlayback(playback, false)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("onCreate")

        val queueManager = QueueManager(
            musicProvider, resources,
            picasso,
            object : MetadataUpdateListener {
                override fun onMetadataChanged(metadata: MediaMetadataCompat) {
                    mSession!!.setMetadata(metadata)
                }

                override fun onMetadataRetrieveError() {
                    mPlaybackManager!!.updatePlaybackState(getString(R.string.error_no_metadata))
                }

                override fun onCurrentQueueIndexUpdated(queueIndex: Int) {
                    mPlaybackManager!!.handlePlayRequest()
                }

                override fun onQueueUpdated(title: String, newQueue: List<MediaSessionCompat.QueueItem>) {
                    mSession!!.setQueue(newQueue)
                    mSession!!.setQueueTitle(title)
                }
            }
        )
        val playback = LocalPlayback(this, musicProvider)
        mPlaybackManager = PlaybackManager(
            this, resources, musicProvider, queueManager,
            playback
        )
        // Start a new MediaSession
        mSession = MediaSessionCompat(this, "MusicService")
        setSessionToken(mSession!!.sessionToken)
        mSession!!.setCallback(mPlaybackManager!!.mediaSessionCallback)
        val context = applicationContext
        val intent = Intent(context, MusicPlayerActivity::class.java)
        val pi = PendingIntent.getActivity(
            context, 99 /*request code*/,
            intent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        mSession!!.setSessionActivity(pi)
        mSessionExtras = Bundle()
        CarHelper.setSlotReservationFlags(mSessionExtras as Bundle, true, true, true)
        mSession!!.setExtras(mSessionExtras)
        mPlaybackManager!!.updatePlaybackState(null)
        videoCastManager.addVideoCastConsumer(mCastConsumer)
        mMediaRouter = MediaRouter.getInstance(applicationContext)

        registerCarConnectionReceiver()
    }

    override fun onStartCommand(startIntent: Intent, flags: Int, startId: Int): Int {
        val action = startIntent.action
        val command = startIntent.getStringExtra(CMD_NAME)
        if (ACTION_CMD == action) {
            if (CMD_PAUSE == command) {
                mPlaybackManager!!.handlePauseRequest()
            } else if (CMD_STOP_CASTING == command) {
                videoCastManager.disconnect()
            }
        } else { // Try to handle the intent as a media button event wrapped by MediaButtonReceiver
            MediaButtonReceiver.handleIntent(mSession, startIntent)
        }

        // Reset the delay handler to enqueue a message to stop the service if
        // nothing is playing.
        mDelayedStopHandler.removeCallbacksAndMessages(null)
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY.toLong())
        return Service.START_STICKY
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        unregisterCarConnectionReceiver()
        // Service is being killed, so make sure we release our resources
        mPlaybackManager!!.handleStopRequest(null)
        mediaNotificationManager.stopNotification()
        videoCastManager.removeVideoCastConsumer(mCastConsumer)
        mDelayedStopHandler.removeCallbacksAndMessages(null)
        mSession!!.release()
        cancel()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        Timber.d(
            "OnGetRoot: clientPackageName=%s clientUid=%s rootHints=%s",
            clientPackageName, clientUid, rootHints
        )
        return BrowserRoot(MediaIdHelper.MEDIA_ID_ROOT, null)
    }

    override fun onLoadChildren(parentMediaId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        launch {
            Timber.d("OnLoadChildren: parentMediaId=%s", parentMediaId)
            withContext(Dispatchers.IO) {
                val media = musicProvider.children(parentMediaId)

                withContext(Dispatchers.Main) {
                    result.sendResult(media)
                }
            }
        }
        result.detach()
    }

    /**
     * Callback method called from PlaybackManager whenever the music is about to play.
     */
    override fun onPlaybackStart() {
        if (!mSession!!.isActive) {
            mSession!!.isActive = true
        }
        mDelayedStopHandler.removeCallbacksAndMessages(null)
        // The service needs to continue running even after the bound client (usually a
        // MediaController) disconnects, otherwise the music playback will stop.
        // Calling startService(Intent) will keep the service running until it is explicitly killed.
        startService(Intent(applicationContext, MusicService::class.java))
    }

    /**
     * Callback method called from PlaybackManager whenever the music stops playing.
     */
    override fun onPlaybackStop() { // Reset the delayed stop handler, so after STOP_DELAY it will be executed again,
        // potentially stopping the service.
        mDelayedStopHandler.removeCallbacksAndMessages(null)
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY.toLong())
        stopForeground(true)
    }

    override fun onPlaybackStateUpdated(newState: PlaybackStateCompat) {
        mSession!!.setPlaybackState(newState)
    }

    override fun onNotificationRequired() {
        launch {
            mediaNotificationManager.startNotification()
        }
    }

    private fun registerCarConnectionReceiver() {
        val filter = IntentFilter(CarHelper.ACTION_MEDIA_STATUS)
        carConnectionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val connectionEvent = intent.getStringExtra(CarHelper.MEDIA_CONNECTION_STATUS)
                isConnectedToCar = CarHelper.MEDIA_CONNECTED == connectionEvent
                Timber.i(
                    "Connection event to Android Auto: %s, isConnectedToCar=%s",
                    connectionEvent, isConnectedToCar
                )
            }
        }
        registerReceiver(carConnectionReceiver, filter)
    }

    private fun unregisterCarConnectionReceiver() {
        unregisterReceiver(carConnectionReceiver)
    }

    /**
     * A simple handler that stops the service if playback is not active (playing)
     */
    private class DelayedStopHandler(service: MusicService) : Handler(Looper.getMainLooper()) {
        private val mWeakReference: WeakReference<MusicService> = WeakReference(service)

        override fun handleMessage(msg: Message) {
            val service = mWeakReference.get()
            if (service != null && service.mPlaybackManager!!.playback != null) {
                if (service.mPlaybackManager!!.playback?.isPlaying!!) {
                    Timber.d("Ignoring delayed stop since the media player is in use.")
                    return
                }
                Timber.d("Stopping service with delay handler.")
                service.stopSelf()
            }
        }
    }

    companion object {
        // Extra on MediaSession that contains the Cast device name currently connected to
        const val EXTRA_CONNECTED_CAST = "com.example.android.uamp.CAST_NAME"

        // The action of the incoming Intent indicating that it contains a command
        // to be executed (see {@link #onStartCommand})
        const val ACTION_CMD = "com.example.android.uamp.ACTION_CMD"

        // The key in the extras of the incoming Intent indicating the command that
        // should be executed (see {@link #onStartCommand})
        const val CMD_NAME = "CMD_NAME"

        // A value of a CMD_NAME key in the extras of the incoming Intent that
        // indicates that the music playback should be paused (see {@link #onStartCommand})
        const val CMD_PAUSE = "CMD_PAUSE"

        // A value of a CMD_NAME key that indicates that the music playback should switch
        // to local playback from cast playback.
        const val CMD_STOP_CASTING = "CMD_STOP_CASTING"

        // Delay stopSelf by using a handler.
        private const val STOP_DELAY = 30000
    }
}
