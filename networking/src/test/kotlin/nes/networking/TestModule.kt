package nes.networking

import dagger.Module
import dagger.Provides
import nes.networking.phishnet.PhishNetApiKey
import okhttp3.Cache
import okhttp3.Interceptor

@Module
interface TestModule {
    companion object {
        @Provides
        fun providesCache(): Cache? = null

        @Provides
        fun providesInterceptors(): Set<@JvmSuppressWildcards Interceptor> = emptySet()

        @Provides
        fun providePhishNetApiKey() = PhishNetApiKey(Config.PHISH_NET_API_KEY)
    }
}
