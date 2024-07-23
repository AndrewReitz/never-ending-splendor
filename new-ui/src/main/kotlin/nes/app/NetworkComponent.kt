package nes.app

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.json.Json
import nes.networking.CACHE_DIR_TAG
import nes.networking.DISK_CACHE_SIZE
import nes.networking.phishin.PHISHIN_API_URL
import nes.networking.phishin.PhishInRepository
import nes.networking.phishin.PhishInService
import nes.networking.phishin.PhishinApiKey
import nes.networking.phishin.PhishinAuthInterceptor
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.kodein.di.instance
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import javax.inject.Named
import javax.inject.Qualifier
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class NetworkComponent {

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
    fun providesInterceptors(): Set<@JvmSuppressWildcards Interceptor> = emptySet()
}

@Qualifier
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class PhishIn