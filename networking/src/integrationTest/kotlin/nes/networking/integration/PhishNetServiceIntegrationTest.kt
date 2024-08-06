package nes.networking.integration

import kotlinx.coroutines.runBlocking
import nes.networking.phishnet.PhishNetService
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhishNetServiceIntegrationTest {

//    @TempDir
//    lateinit var tempDir: File
//
//    override val di = DI.lazy {
//        import(networkingModule, true)
//
//        bind<File>(tag = CACHE_DIR_TAG) with singleton { tempDir }
//
//        inBindSet<Interceptor> {
//            add {
//                singleton {
//                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
//                }
//            }
//        }
//    }

//    private val classUnderTest: PhishNetService

    @Test
    fun `request show info for 2020-02-22`() = runBlocking {
//        classUnderTest.setlist("2020-02-22").run {
//            assertEquals("", error_message, "Error message should be empty")
//            assertEquals(expected = "2020-02-22", actual = data[0].showdate)
//        }
    }

    @Test
    fun `request show id for 1560881138`() = runBlocking {
        // 1560881138 is the 2020-02-22 showid
//        classUnderTest.reviews("1560881138").run {
//            assertEquals("", error_message, "Error message should be empty")
//            assertTrue(data.isNotEmpty())
//        }
    }
}
