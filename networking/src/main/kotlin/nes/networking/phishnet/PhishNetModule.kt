package nes.networking.phishnet

import dagger.Module
import dagger.Provides
import kotlinx.serialization.json.Json
import nes.networking.NetworkingModule
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.BINARY

@Qualifier
@Retention(BINARY)
annotation class PhishNet

@Module(includes = [NetworkingModule::class])
interface PhishNetModule {
    companion object {

        val PHISH_NET_API_URL: HttpUrl = requireNotNull("https://api.phish.net/v5/".toHttpUrlOrNull())

        @Provides
        fun providesPhishNetService(
            @PhishNet retrofit: Retrofit
        ): PhishNetService = retrofit.create(PhishNetService::class.java)

        @PhishNet
        @Provides
        internal fun providePhishNetRetrofit(
            okHttpClient: OkHttpClient,
            authInterceptor: PhishNetAuthInterceptor,
            json: Json
        ): Retrofit = Retrofit.Builder()
            .client(
                okHttpClient
                    .newBuilder()
                    .addInterceptor(authInterceptor)
                    .build()
            )
            .addConverterFactory(
                json.asConverterFactory("application/json; charset=UTF8".toMediaType())
            )
            .baseUrl(PHISH_NET_API_URL)
            .build()
    }
}
