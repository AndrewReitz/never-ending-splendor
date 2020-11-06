package never.ending.splendor.app

import timber.log.Timber

/** Debug specific initialization things. */
class DebugAppInitializer() : AppInitializer {
    override fun invoke() {
        Timber.plant(Timber.DebugTree())
    }
}
