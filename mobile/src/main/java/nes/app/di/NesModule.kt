package nes.app.di

import android.content.Context
import coil.ImageLoader
import com.jakewharton.byteunits.DecimalByteUnit.MEGABYTES
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import nes.app.R
import nes.app.playback.MediaPlayerContainer
import nes.app.playback.RealMediaPlayerContainer
import nes.app.ui.ApiErrorMessage
import nes.networking.phishin.PhishInModule
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.time.Duration.Companion.hours

@InstallIn(SingletonComponent::class)
@Module(includes = [PhishInModule::class])
interface NesModule {
    companion object {
        private val DISK_CACHE_SIZE = MEGABYTES.toBytes(512)

        @Provides
        @Singleton
        fun provideImageLoader(
            @ApplicationContext context: Context,
            okHttpClient: OkHttpClient
        ) = ImageLoader.Builder(context)
            .okHttpClient(okHttpClient)
            .crossfade(true)
            .build()

        @Provides
        fun provideCache(
            @ApplicationContext context: Context
        ): Cache = Cache(File(context.cacheDir, "http"), DISK_CACHE_SIZE)

        @Provides
        @Singleton
        fun provideApiErrorMessage(
            @ApplicationContext context: Context
        ): ApiErrorMessage = ApiErrorMessage(
            context.getString(R.string.api_error_message)
        )

        @Provides
        @IntoSet
        fun provideCacheOverrideInterceptor(): Interceptor {
            // override the max-age=0 set by the server
            // in an attempt to speed up this app
            // and not have android auto (MediaItemTree) spam the server with multiple request
            return Interceptor { chain ->
                val originalResponse = chain.proceed(chain.request())

                val headers = originalResponse.headers.newBuilder()
                    .removeAll("Cache-Control")
                    .build()

                originalResponse.newBuilder()
                    .headers(headers)
                    .header("Cache-Control", "max-age=${1.hours.inWholeSeconds}")
                    .build()
            }
        }
    }

    @Binds
    fun bindsMediaControllerContainer(container: RealMediaPlayerContainer): MediaPlayerContainer
}
