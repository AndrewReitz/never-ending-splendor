package nes.app.playback

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Container for the MediaController basically used to get the
 * media controller into the dependency graph.
 */
@OptIn(UnstableApi::class)
@Singleton
class MediaControllerContainer @Inject constructor(
    @ApplicationContext val context: Context,
) {
    private val controllerFuture: ListenableFuture<MediaController>
    private lateinit var _mediaController: MediaController

    val mediaController by lazy { _mediaController }

    init {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = mediaControllerFuture

        mediaControllerFuture.addListener(
            {
                _mediaController = mediaControllerFuture.get()
            },
            MoreExecutors.directExecutor()
        )
    }
}