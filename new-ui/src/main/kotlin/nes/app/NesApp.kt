package nes.app

import android.content.ComponentName
import androidx.annotation.OptIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.compose.rememberNavController
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
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

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> {
                    val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
                    val mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
                    controllerFuture = mediaControllerFuture
                    mediaControllerFuture.addListener(
                        { mediaController = mediaControllerFuture.get() },
                        MoreExecutors.directExecutor()
                    )
                }

                Lifecycle.Event.ON_DESTROY -> {
                    controllerFuture?.let { MediaController.releaseFuture(it) }
                    mediaController = null
                }

                Lifecycle.Event.ON_START -> {}
                Lifecycle.Event.ON_RESUME -> {}
                Lifecycle.Event.ON_PAUSE -> {}
                Lifecycle.Event.ON_STOP -> {}
                Lifecycle.Event.ON_ANY -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val navController = rememberNavController()
    NesNavController(
        musicPlayer = mediaController,
        navController = navController
    )
}
