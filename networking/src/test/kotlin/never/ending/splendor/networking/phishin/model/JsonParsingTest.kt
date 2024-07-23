package nes.networking.phishin.model

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import nes.networking.networkingModule
import never.ending.splendor.networking.showJson
import never.ending.splendor.networking.showsJson
import never.ending.splendor.networking.yearsJson
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import kotlin.test.Test

class JsonParsingTest : DIAware {

    override val di = DI.lazy {
        import(networkingModule)
    }

    private val json: Json by instance()

    @Test
    fun `should parse years`() {
        val result = json.decodeFromString<SuccessfulResponse<List<YearData>>>(yearsJson.readUtf8())
        assertThat(result.data).isInstanceOf(List::class.java)
    }

    @Test
    fun `should parse shows`() {
        val result = json.decodeFromString<SuccessfulResponse<List<Show>>>(showsJson.readUtf8())
        assertThat(result.data).isInstanceOf(List::class.java)
    }

    @Test
    fun `should parse show`() {
        val result = json.decodeFromString<SuccessfulResponse<Show>>(showJson.readUtf8())
        assertThat(result.data).isInstanceOf(Show::class.java)
    }
}
