package nes.networking.phishin

import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

const val PHISHIN_RETROFIT_TAG = "Phishin"
val PHISHIN_API_URL: HttpUrl = requireNotNull("https://phish.in/".toHttpUrlOrNull())

val phishInModule = DI.Module(name = "PhishInModule") {

    bind<PhishinApiKey>() with singleton {
        PhishinApiKey(Config.PHISH_IN_API_KEY)
    }

    bind<PhishInService>() with singleton {
        instance<Retrofit>(tag = PHISHIN_RETROFIT_TAG).create(PhishInService::class.java)
    }

    bind<PhishInRepository>() with singleton { PhishInRepository(instance()) }

    bind<Retrofit>(tag = PHISHIN_RETROFIT_TAG) with singleton {
        Retrofit.Builder()
            .client(
                instance<OkHttpClient>()
                    .newBuilder()
                    .addInterceptor(PhishinAuthInterceptor(instance()))
                    .build()
            )
            .addConverterFactory(
                 instance<Json>().asConverterFactory("application/json; charset=UTF8".toMediaType())
            )
            .baseUrl(PHISHIN_API_URL)
            .build()
    }
}
