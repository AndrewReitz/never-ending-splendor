package never.ending.splendor.app

import com.facebook.flipper.plugins.network.FlipperOkhttpInterceptor
import com.facebook.flipper.plugins.network.NetworkFlipperPlugin
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import org.kodein.di.DI
import org.kodein.di.DI.Module
import org.kodein.di.bind
import org.kodein.di.inSet
import org.kodein.di.instance
import org.kodein.di.singleton
import timber.log.Timber

val debugModule = DI.Module("Debug Module") {
    bind<AppInitializer>() with singleton { DebugAppInitializer(instance(), instance()) }
    bind<Interceptor>().inSet() with singleton {
        HttpLoggingInterceptor { message ->
            Timber.tag("OkHttpClient")
            Timber.v(message)
        }.apply { level = HttpLoggingInterceptor.Level.BODY }
    }
    bind<Interceptor>().inSet() with singleton {
        FlipperOkhttpInterceptor(instance())
    }
    bind<NetworkFlipperPlugin>() with instance(NetworkFlipperPlugin())
}

val buildSpecificModules = listOf(debugModule)
