package nes.networking.integration

import Config
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import nes.networking.NetworkingModule
import nes.networking.phishin.PhishInModule
import nes.networking.phishnet.PhishNetApiKey
import nes.networking.phishnet.PhishNetModule
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor

@Module(includes = [PhishNetModule::class, PhishInModule::class])
interface IntegrationTestModule {
    companion object {
        @Provides
        fun providesCache(): Cache? = null

        @Provides
        @IntoSet
        fun providesInterceptors(): Interceptor = HttpLoggingInterceptor()
            .apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

        @Provides
        fun providePhishNetApiKey() = PhishNetApiKey(Config.PHISH_NET_API_KEY)
    }
}
