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
import timber.log.Timber

@InstallIn(SingletonComponent::class)
@Module
class DebugComponent {

    @Provides
    @IntoSet
    fun provideHttpLoggingInterceptor(): Interceptor {
        return HttpLoggingInterceptor { message ->
            Timber.tag("OkHttpClient")
            Timber.v(message)
        }.apply { level = HttpLoggingInterceptor.Level.BASIC }
    }

    @Provides
    @IntoSet
    fun provideFlipperInterceptor(networkFlipperPlugin: NetworkFlipperPlugin): Interceptor {
        return FlipperOkhttpInterceptor(networkFlipperPlugin)
    }

    @Provides
    fun provideNetworkFlipperPlugin(): NetworkFlipperPlugin = NetworkFlipperPlugin()
}

