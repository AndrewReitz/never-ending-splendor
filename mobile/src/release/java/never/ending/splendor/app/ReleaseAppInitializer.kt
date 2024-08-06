package nes.app

import timber.log.Timber
import javax.inject.Inject
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

/** Release specific initialization things. */
class ReleaseAppInitializer @Inject constructor(private val crashlyticsTree: CrashlyticsTree) : AppInitializer {
    override fun invoke() {
        Timber.plant(crashlyticsTree)
    }
}

@InstallIn(SingletonComponent::class)
@Module
abstract class ReleaseAppInitializerModule {
    @Binds
    abstract fun providesAppInitializer(releaseAppInitializer: ReleaseAppInitializer): AppInitializer
}

