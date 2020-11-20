package never.ending.splendor.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager.IMPORTANCE_LOW
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.libraries.cast.companionlibrary.cast.CastConfiguration
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import nes.networking.CACHE_DIR_TAG
import nes.networking.networkingModule
import never.ending.splendor.R
import never.ending.splendor.app.model.MusicProvider
import never.ending.splendor.app.model.MusicProviderSource
import never.ending.splendor.app.model.PhishProviderSource
import never.ending.splendor.app.ui.FullScreenPlayerActivity
import okhttp3.OkHttpClient
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.android.androidCoreModule
import org.kodein.di.android.x.androidXModule
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton
import timber.log.Timber
import java.io.File

const val MEDIA_PLAYER_NOTIFICATION = "MediaPlayer"

class RoboPhishApplication : Application(), DIAware {

    override val di = DI.lazy {
        import(androidCoreModule(this@RoboPhishApplication))
        import(androidXModule(this@RoboPhishApplication))
        import(networkingModule)

        /**
         * Each build variant should provide a list of modules it would like to provide, this
         * allows for these variants to provide different functionality, based on the the
         * build type. For example, [Timber] logging w/ the [Timber.DebugTree] should only
         * be done in the debug module.
         */
        buildSpecificModules.forEach { module ->
            import(module)
        }

        bind<NotificationManagerCompat>() with singleton { NotificationManagerCompat.from(instance()) }
        bind<VideoCastManager>() with singleton { VideoCastManager.getInstance() }

        bind<GoogleApiAvailability>() with singleton { GoogleApiAvailability.getInstance() }

        bind<File>(tag = CACHE_DIR_TAG) with singleton { instance<Context>().cacheDir }

        bind<MusicProvider>() with singleton { MusicProvider(instance(), instance()) }
        bind<MusicProviderSource>() with singleton { PhishProviderSource(instance(), instance()) }

        bind<Picasso>() with singleton {
            Picasso.Builder(instance())
                .downloader(OkHttp3Downloader(instance<OkHttpClient>()))
                .listener { _, uri, exception ->
                    Timber.e(exception, "Error while loading image %s", uri)
                }
                .build()
        }

        bind<PackageValidator>() with singleton {
            PackageValidator(instance())
        }
    }

    private val appInitializer: AppInitializer by instance()
    private val notificationManagerCompat: NotificationManagerCompat by instance()

    override fun onCreate() {
        super.onCreate()

        appInitializer()

        val applicationId = resources.getString(R.string.cast_application_id)
        VideoCastManager.initialize(
            applicationContext,
            CastConfiguration.Builder(applicationId)
                .enableWifiReconnection()
                .enableAutoReconnect()
                .enableDebug()
                .setTargetActivity(FullScreenPlayerActivity::class.java)
                .build()
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManagerCompat.createNotificationChannel(
                NotificationChannel(
                    MEDIA_PLAYER_NOTIFICATION,
                    MEDIA_PLAYER_NOTIFICATION,
                    IMPORTANCE_LOW
                )
            )
        }
    }
}
