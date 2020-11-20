package nes.networking.integration

import kotlinx.coroutines.runBlocking
import nes.networking.CACHE_DIR_TAG
import nes.networking.networkingModule
import nes.networking.phishin.PhishInService
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Test
import org.junit.jupiter.api.io.TempDir
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.bind
import org.kodein.di.inSet
import org.kodein.di.instance
import org.kodein.di.singleton
import java.io.File
import kotlin.test.assertTrue

class PhishInServiceIntegrationTest : DIAware {

    @TempDir
    lateinit var tempDir: File

    override val di = DI.lazy {
        import(networkingModule)

        bind<File>(tag = CACHE_DIR_TAG) with singleton { tempDir }

        bind<Interceptor>().inSet() with singleton {
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        }
    }

    private val classUnderTest: PhishInService by instance()

    @Test
    fun `should get years`() = runBlocking {
        assertTrue(classUnderTest.years().data.isNotEmpty())
    }
}
