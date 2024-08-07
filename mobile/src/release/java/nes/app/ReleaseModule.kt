package nes.app

import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor

@InstallIn(SingletonComponent::class)
@Module
interface ReleaseModule {
    @Binds abstract fun providesAppInitializer(releaseAppInitializer: ReleaseAppInitializer): AppInitializer

    companion object {
        @Provides
        fun providesFirebaseCrashlytics(): FirebaseCrashlytics = FirebaseCrashlytics.getInstance()

        @Provides
        fun providesInterceptors(): Set<@JvmSuppressWildcards Interceptor> = emptySet()
    }
}
