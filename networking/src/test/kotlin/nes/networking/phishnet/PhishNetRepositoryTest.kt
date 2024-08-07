package nes.networking.phishnet

import dagger.Component
import dev.forkhandles.result4k.valueOrNull
import kotlinx.coroutines.runBlocking
import nes.networking.TestModule
import nes.networking.reviews
import nes.networking.setlist
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.test.assertNotNull

class PhishNetRepositoryTest {

    private val mockWebServer = MockWebServer()

    @Inject
    lateinit var classUnderTest: PhishNetRepository

    @BeforeEach
    fun init() {
        DaggerPhishNetTestDeps.create().inject(this)
    }

    @Test
    fun `should get setlists`() = runBlocking<Unit> {
        mockWebServer.enqueue(MockResponse().setBody(setlist.buffer))

        classUnderTest.setlist("2020-02-22").run {
            assertNotNull(valueOrNull())
        }
    }

    @Test
    fun `should get reviews`() = runBlocking<Unit> {
        mockWebServer.enqueue(MockResponse().setBody(reviews.buffer))

        classUnderTest.reviews("1560881138").run {
            assertNotNull(valueOrNull())
        }
    }
}

@Singleton
@Component(modules = [TestModule::class, PhishNetModule::class])
interface PhishNetTestDeps {
    fun inject(test: PhishNetRepositoryTest)
}

