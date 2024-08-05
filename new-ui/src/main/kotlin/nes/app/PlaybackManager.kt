package nes.app

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import nes.app.service.PlaybackService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackManager @OptIn(UnstableApi::class) @Inject constructor(
    @ApplicationContext val context: Context,
) {

    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val controllerFuture: ListenableFuture<MediaController>
    private lateinit var mediaController: MediaController

    init {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = mediaControllerFuture

        mediaControllerFuture.addListener(
            {
                mediaController = mediaControllerFuture.get()
            },
            MoreExecutors.directExecutor()
        )
    }

    fun release() {
        MediaController.releaseFuture(controllerFuture)
    }
}