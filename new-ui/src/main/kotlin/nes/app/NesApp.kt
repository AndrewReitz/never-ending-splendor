package nes.app

import android.content.ComponentName
import androidx.annotation.OptIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.android.gms.cast.framework.CastContext
import androidx.navigation.compose.rememberNavController
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import nes.app.service.PlaybackService

@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Composable
fun NesApp(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
) {
    val context = LocalContext.current
    var controllerFuture by remember { mutableStateOf<ListenableFuture<MediaController>?>(null) }
    var mediaController by remember { mutableStateOf<MediaController?>(null) }
    var castPlayer by remember { mutableStateOf<CastPlayer?>(null) }
    var musicPlayer by remember { mutableStateOf<Player?>(null) }

    var isConnected by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    val sessionToken =
                        SessionToken(context, ComponentName(context, PlaybackService::class.java))
                    val cf = MediaController.Builder(context, sessionToken)
                        .buildAsync()
                    controllerFuture = cf

                    cf.addListener(
                        {
                            mediaController = cf.get()
                            musicPlayer = mediaController
                        },
                        MoreExecutors.directExecutor()
                    )
                }

                Lifecycle.Event.ON_STOP -> {
                    controllerFuture?.let { MediaController.releaseFuture(it) }
                    mediaController = null
                }

                Lifecycle.Event.ON_CREATE -> {
                    CastContext.getSharedInstance(context, MoreExecutors.directExecutor())
                        .addOnFailureListener {
                            it.printStackTrace()
                        }
                        .addOnSuccessListener {
                            castPlayer = CastPlayer(it).apply {
                                setSessionAvailabilityListener(
                                    object : SessionAvailabilityListener {
                                        override fun onCastSessionAvailable() {
                                            isConnected = true

//                                            mediaController?.let { mc ->
//                                                for (i in 0 until mc.mediaItemCount) {
//                                                    val mediaItem = mc.getMediaItemAt(i)
//                                                    castPlayer?.addMediaItem(i, mediaItem)
//                                                }
//                                                castPlayer?.prepare()
//                                                mc.clearMediaItems()
//                                                if (mc.isPlaying) {
//                                                    mc.pause()
//                                                    castPlayer?.play()
//                                                }
//                                            }
                                            musicPlayer = castPlayer
                                        }

                                        override fun onCastSessionUnavailable() {
//                                            castPlayer?.let { player ->
//                                                for (i in 0 until player.mediaItemCount) {
//                                                    val mediaItem = player.getMediaItemAt(i)
//                                                    mediaController?.addMediaItem(i, mediaItem)
//                                                }
//                                                mediaController?.prepare()
//                                                player.clearMediaItems()
//                                                if (player.isPlaying) {
//                                                    player.pause()
//                                                    mediaController?.play()
//                                                }
//                                            }
//                                            musicPlayer = mediaController
                                        }
                                    }
                                )
                            }
                        }
                }

                Lifecycle.Event.ON_RESUME -> {}
                Lifecycle.Event.ON_PAUSE -> {}
                Lifecycle.Event.ON_DESTROY -> {
                    castPlayer?.setSessionAvailabilityListener(null)
                    castPlayer?.release()
                    castPlayer = null
                }

                Lifecycle.Event.ON_ANY -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val navController = rememberNavController()
    NesNavController(
        musicPlayer = musicPlayer,
        navController = navController
    )
}
