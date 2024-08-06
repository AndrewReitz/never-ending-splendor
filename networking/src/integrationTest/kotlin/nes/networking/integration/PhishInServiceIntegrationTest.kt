package nes.networking.integration

import kotlinx.coroutines.runBlocking
import nes.networking.phishin.PhishInService
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue

class PhishInServiceIntegrationTest {

//    @TempDir
//    lateinit var tempDir: File
//
//    override val di = DI.lazy {
//        import(networkingModule)
//
//        bind<File>(tag = CACHE_DIR_TAG) with singleton { tempDir }
//
//        bindSet<Interceptor> {
//            bind {
//                singleton {
//                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
//                }
//            }
//        }
//    }

//    private val classUnderTest: PhishInService by instance()

    @Test
    fun `should get years`() = runBlocking {
//        assertTrue(classUnderTest.years().data.isNotEmpty())
    }
}
