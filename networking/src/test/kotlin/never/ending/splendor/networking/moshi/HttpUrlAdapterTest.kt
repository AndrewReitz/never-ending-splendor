package nes.networking.moshi

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import nes.networking.networkingModule
import nes.networking.phishin.model.Track
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

/**
 * Test both json adapters in the moshi package.
 */
class HttpUrlAdapterTest : DIAware {

    private val moshi: Moshi by instance()

    @Test
    fun `should be bijective`() {

        val testData = Track(
            id = "Rift",
            title = "Rift",
            mp3 = "http://example.com".toHttpUrl(),
            duration = 10L
        )

        val classUnderTest = moshi.adapter(Track::class.java)
        val json = classUnderTest.toJson(testData)
        val result = classUnderTest.fromJson(json)

        assertThat(result).isEqualTo(testData)
    }

    override val di = DI.lazy {
        import(networkingModule)
    }
}
