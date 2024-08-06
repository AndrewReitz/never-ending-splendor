package nes.app.di

import android.content.Context
import coil.ImageLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import nes.networking.NetworkingModule.Companion.DISK_CACHE_SIZE
import nes.networking.NetworkingModule.Companion.PHISHIN_API_URL
import nes.networking.phishin.PhishInRepository
import nes.networking.phishin.PhishInService
import nes.networking.phishin.PhishinApiKey
import nes.networking.phishin.PhishinAuthInterceptor
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class NetworkModule {

    @Singleton
    @Provides
    fun provideJson() = Json {
        ignoreUnknownKeys = true
    }

    @Singleton
    @Provides
    fun providesOkhttpClient(
        @ApplicationContext context: Context,
        interceptors: Set<@JvmSuppressWildcards Interceptor>
    ) = OkHttpClient.Builder()
        .cache(Cache(File(context.cacheDir, "http"), DISK_CACHE_SIZE.toLong()))
        .apply { interceptors.forEach { addInterceptor(it) } }
        .build()

    @Singleton
    @Provides
    fun providePhishInRepository(phishInService: PhishInService) = PhishInRepository(phishInService)

    @Singleton
    @Provides
    fun providePhishInService(
        @PhishIn retrofit: Retrofit
    ): PhishInService = retrofit.create(PhishInService::class.java)

    @Singleton
    @Provides
    @PhishIn
    fun providesPhishInRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
        phishInApiKey: PhishinApiKey
    ): Retrofit = Retrofit.Builder()
        .client(
            okHttpClient
                .newBuilder()
                .addInterceptor(PhishinAuthInterceptor(phishInApiKey))
                .build()
        )
        .addConverterFactory(
            json.asConverterFactory("application/json; charset=UTF8".toMediaType())
        )
        .baseUrl(PHISHIN_API_URL)
        .build()

    @Singleton
    @Provides
    fun providesPhishInApiKey() =  PhishinApiKey(Config.PHISH_IN_API_KEY)

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ) = ImageLoader.Builder(context)
        .okHttpClient(okHttpClient)
        .crossfade(true)
        .build()
}

@Qualifier
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class PhishIn