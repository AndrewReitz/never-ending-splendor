package never.ending.splendor.app

import never.ending.splendor.app.AppInitializer
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber

/** Debug specific initialization things. */
class DebugAppInitializer(private val interceptors: MutableList<Interceptor>): AppInitializer {
    override fun invoke() {
        Timber.plant(Timber.DebugTree())
        interceptors.add(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
    }
}
