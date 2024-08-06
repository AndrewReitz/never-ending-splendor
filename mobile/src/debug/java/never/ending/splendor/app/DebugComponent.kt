package never.ending.splendor.app

import com.facebook.flipper.plugins.network.FlipperOkhttpInterceptor
import com.facebook.flipper.plugins.network.NetworkFlipperPlugin
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.inBindSet
import org.kodein.di.instance
import org.kodein.di.singleton
import timber.log.Timber

@InstallIn(SingletonComponent::class)
@Module
class DebugComponent {

    @Provides
    @IntoSet
    fun provideHttpLoggingInterceptors(): Interceptor {
        return HttpLoggingInterceptor { message ->
            Timber.tag("OkHttpClient")
            Timber.v(message)
        }.apply { level = HttpLoggingInterceptor.Level.BODY }
    }

    @Provides
    @IntoSet
    fun provideFlipperInterceptor(networkFlipperPlugin: NetworkFlipperPlugin): Interceptor {
        return FlipperOkhttpInterceptor(networkFlipperPlugin)
    }

    @Provides
    fun provideNetworkFlipperPlugin(): NetworkFlipperPlugin = NetworkFlipperPlugin()
}

