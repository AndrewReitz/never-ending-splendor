package nes.app

import android.content.ComponentName
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
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.compose.rememberNavController
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import nes.app.service.PlaybackService

@Composable
fun NesApp(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
) {
    val context = LocalContext.current
    var controllerFuture by remember { mutableStateOf<ListenableFuture<MediaController>?>(null) }
    var mediaController by remember { mutableStateOf<MediaController?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
                val cf = MediaController.Builder(context, sessionToken)
                    .buildAsync()
                controllerFuture = cf

                cf.addListener({ mediaController = cf.get() }, MoreExecutors.directExecutor())
            } else if (event == Lifecycle.Event.ON_STOP) {
                controllerFuture?.let { MediaController.releaseFuture(it) }
                mediaController = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }


    val navController = rememberNavController()
    NesNavController(
        mediaController = mediaController,
        navController = navController
    )
}
