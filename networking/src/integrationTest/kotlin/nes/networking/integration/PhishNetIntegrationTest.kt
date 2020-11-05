package nes.networking.integration

import kotlinx.coroutines.runBlocking
import never.ending.splendor.networking.CACHE_DIR_TAG
import never.ending.splendor.networking.PHISH_NET_RETROFIT_TAG
import never.ending.splendor.networking.networkingModule
import never.ending.splendor.networking.phishnet.PhishNetApiKey
import never.ending.splendor.networking.phishnet.PhishNetService
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.bind
import org.kodein.di.inSet
import org.kodein.di.instance
import org.kodein.di.singleton
import retrofit2.Retrofit
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhishNetIntegrationTest : DIAware {

    @TempDir
    lateinit var tempDir: File

    override val di = DI.lazy {
        import(networkingModule)

        bind<File>(tag = CACHE_DIR_TAG) with singleton { tempDir }
        bind<PhishNetApiKey>() with singleton {
            PhishNetApiKey("FFC6B556891FFF7A4198")
        }

        bind<Interceptor>().inSet() with singleton {
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        }
    }

    @Test
    fun `request show info for 2020-02-22`() = runBlocking<Unit> {
        // todo just inject from kodien
        val retrofit: Retrofit by instance(PHISH_NET_RETROFIT_TAG)
        val classUnderTest = retrofit.create(PhishNetService::class.java)

        classUnderTest.setlist("2020-02-22").run {
            assertNull(error_message, "Error message should be null")
            assertTrue("only one show should be returned") { response.data.size == 1 }
            assertEquals(expected = "2020-02-22", actual = response.data[0].showdate)
        }
    }

    @Test
    fun `request show id for 1560881138`() = runBlocking<Unit> {
        // todo just inject from kodien
        val retrofit: Retrofit by instance(PHISH_NET_RETROFIT_TAG)
        val classUnderTest = retrofit.create(PhishNetService::class.java)

        // 1560881138 is the 2020-02-22 showid
        classUnderTest.reviews("1560881138").run {
            assertNull(error_message, "Error message should be null")
            assertTrue(response.data.isNotEmpty())
        }
    }
}
