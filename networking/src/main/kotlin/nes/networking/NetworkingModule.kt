package nes.networking

import dagger.Module
import dagger.Provides
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
interface NetworkingModule {
    companion object {
        val PHISHIN_API_URL: HttpUrl = requireNotNull("https://phish.in/".toHttpUrlOrNull())

        @Singleton
        @Provides
        fun provideJson() = Json {
            ignoreUnknownKeys = true
        }

        @Singleton
        @Provides
        fun providesOkhttpClient(
            cache: Cache?,
            interceptors: Set<@JvmSuppressWildcards Interceptor>
        ) = OkHttpClient.Builder()
            .cache(cache)
            .apply { interceptors.forEach { addInterceptor(it) } }
            .build()
    }
}
