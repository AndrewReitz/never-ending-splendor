package never.ending.splendor.networking.phishin.model

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import never.ending.splendor.networking.networkingModule
import org.junit.jupiter.api.Test
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

/**
 * Test to ensure the models match what we expect from the [robophish.PhishinService], and
 * that the adapters are generated and setup correctly in [Moshi].
 */
class JsonParsingTest : DIAware {

    override val di = DI.lazy {
        import(networkingModule)
    }

    private val moshi: Moshi by instance()

    @Test
    fun `should parse years`() {
        val listYearType = Types.newParameterizedType(List::class.java, YearData::class.java)
        val successListYearType = Types.newParameterizedType(SuccessfulResponse::class.java, listYearType)

        val adapter: JsonAdapter<SuccessfulResponse<List<YearData>>> = moshi.adapter(successListYearType)

        val result = adapter.fromJson(yearsJson)!!
        assertThat(result.data).isInstanceOf(List::class.java)
    }

    @Test
    fun `should parse shows`() {
        val listShowType = Types.newParameterizedType(List::class.java, Show::class.java)
        val successListShowType = Types.newParameterizedType(SuccessfulResponse::class.java, listShowType)
        val adapter: JsonAdapter<SuccessfulResponse<List<Show>>> = moshi.adapter(successListShowType)

        val result = adapter.fromJson(showsJson)!!
        assertThat(result.data).isInstanceOf(List::class.java)
    }

    @Test
    fun `should parse show`() {
        val successShowType = Types.newParameterizedType(SuccessfulResponse::class.java, Show::class.java)
        val adapter: JsonAdapter<SuccessfulResponse<Show>> = moshi.adapter(successShowType)

        val result = adapter.fromJson(showJson)!!
        assertThat(result.data).isInstanceOf(Show::class.java)
    }
}
