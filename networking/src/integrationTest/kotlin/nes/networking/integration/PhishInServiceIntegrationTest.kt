package nes.networking.integration

import kotlinx.coroutines.runBlocking
import nes.networking.phishin.PhishInService
import org.junit.Test
import org.junit.jupiter.api.BeforeEach
import javax.inject.Inject
import kotlin.test.assertTrue

class PhishInServiceIntegrationTest {

    @Inject
    lateinit var classUnderTest: PhishInService

    @BeforeEach
    fun init() {
        DaggerIntegrationTestComponent.create().inject(this)
    }

    @Test
    fun `should get years`() = runBlocking {
        assertTrue(classUnderTest.years().data.isNotEmpty())
    }
}
