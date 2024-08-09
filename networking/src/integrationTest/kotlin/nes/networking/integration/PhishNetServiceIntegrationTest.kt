package nes.networking.integration

import kotlinx.coroutines.test.runTest
import nes.networking.phishnet.PhishNetService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhishNetServiceIntegrationTest {

    @Inject
    lateinit var classUnderTest: PhishNetService

    @BeforeEach
    fun init() {
        DaggerIntegrationTestComponent.create().inject(this)
    }

    @Test
    fun `request show info for 2020-02-22`() = runTest {
        classUnderTest.setlist("2020-02-22").run {
            assertEquals("", error_message, "Error message should be empty")
            assertEquals(expected = "2020-02-22", actual = data[0].showdate)
        }
    }

    @Test
    fun `request show id for 1560881138`() = runTest {
        // 1560881138 is the 2020-02-22 showid
        classUnderTest.reviews("1560881138").run {
            assertEquals("", error_message, "Error message should be empty")
            assertTrue(data.isNotEmpty())
        }
    }
}
