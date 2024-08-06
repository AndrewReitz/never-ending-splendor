package nes.app.playback

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.android.gms.cast.framework.CastContext
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@UnstableApi
class PlaybackService : MediaSessionService(), SessionAvailabilityListener, MediaLibraryService.MediaLibrarySession.Callback {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var player: ReplaceableForwardingPlayer? = null
    private var mediaSession: MediaSession? = null
    private var exoPlayer: Player? = null
    private var castPlayer: Player? = null


    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        val castContext = CastContext.getSharedInstance(this, MoreExecutors.directExecutor())
            .addOnFailureListener { /*TODO Log errors*/ }
            .result


        castPlayer = CastPlayer(castContext).apply {
            setSessionAvailabilityListener(this@PlaybackService)
        }

        val player = ReplaceableForwardingPlayer(exoPlayer)
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(this)
            .build()
        this.exoPlayer = exoPlayer
        this.player = player
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? = mediaSession

    // User dismissed the app from recent tasks
    override fun onTaskRemoved(rootIntent: Intent?) {
        mediaSession?.player?.run {
            if (playWhenReady || mediaItemCount == 0 || playbackState == Player.STATE_ENDED) {
                // stop player if it's not playing otherwise allow it to continue playing
                // in the background
                stopSelf()
            }
        }
    }

    override fun onCastSessionAvailable() {
        castPlayer?.let {
            player?.setPlayer(it)
        }
    }

    override fun onCastSessionUnavailable() {
        exoPlayer?.let {
            player?.setPlayer(it)
        }
    }


    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        val settable = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
        serviceScope.launch {
            settable.set(
                MediaSession.MediaItemsWithStartPosition(
                    player?.playlist ?: emptyList(),
                    player?.currentPlaylistIndex ?: 0,
                    player?.currentPosition ?: C.TIME_UNSET
                )
            )
        }
        return settable
    }
}