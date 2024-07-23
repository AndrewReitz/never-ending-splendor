package never.ending.splendor.networking.phishnet

import dev.forkhandles.result4k.valueOrNull
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import nes.networking.phishnet.PHISH_NET_URL_TAG
import nes.networking.phishnet.PhishNetRepository
import never.ending.splendor.networking.reviews
import never.ending.splendor.networking.setlist
import okhttp3.ExperimentalOkHttpApi
import okhttp3.HttpUrl
import org.junit.Test
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton
import kotlin.test.assertNotNull

@OptIn(ExperimentalOkHttpApi::class)
class PhishNetRepositoryTest: DIAware {

    override val di = DI.lazy {
        bind<HttpUrl>(tag = PHISH_NET_URL_TAG) with singleton {
            instance<MockWebServer>().url("/")
        }
        bind<MockWebServer>() with singleton { MockWebServer() }
    }

    private val mockWebServer: MockWebServer by instance()
    private val classUnderTest: PhishNetRepository by instance()

    @Test
    fun `should get setlists`() = runBlocking<Unit> {
        mockWebServer.enqueue(
            MockResponse.Builder()
                .body(setlist.buffer)
                .build()
        )

        classUnderTest.setlist("2020-02-22").run {
            assertNotNull(valueOrNull())
        }
    }

    @Test
    fun `should get reviews`() = runBlocking<Unit> {
        mockWebServer.enqueue(
            MockResponse.Builder()
            .body(reviews.buffer)
            .build()
        )

        classUnderTest.reviews("1560881138").run {
            assertNotNull(valueOrNull())
        }
    }
}
