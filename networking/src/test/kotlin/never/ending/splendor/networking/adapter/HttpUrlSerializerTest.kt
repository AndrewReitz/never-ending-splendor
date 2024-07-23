package never.ending.splendor.networking.adapter

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nes.networking.networkingModule
import nes.networking.phishin.model.Track
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import kotlin.test.Test

class HttpUrlSerializerTest : DIAware {

    private val json: Json by instance()

    @Test
    fun `should be bijective`() {

        val testData = Track(
            id = 12345,
            title = "Rift",
            mp3 = "http://example.com".toHttpUrl(),
            duration = 10L
        )

        val jsonVale = json.encodeToString(testData)
        val result = json.decodeFromString<Track>(jsonVale)

        assertThat(result).isEqualTo(testData)
    }

    override val di = DI.lazy {
        import(networkingModule)
    }
}
