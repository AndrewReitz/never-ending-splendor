package nes.networking.phishnet

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

const val PHISH_NET_RETROFIT_TAG = "phish.net"
const val PHISH_NET_URL_TAG = PHISH_NET_RETROFIT_TAG

private val PHISH_NET_API_URL: HttpUrl = requireNotNull("https://api.phish.net/v5/".toHttpUrlOrNull())

val phishNetModule = DI.Module(name = "PhishNetModule") {
    bind<PhishNetApiKey>() with singleton {
        PhishNetApiKey(Config.PHISH_NET_API_KEY)
    }

    bind<Retrofit>(tag = PHISH_NET_RETROFIT_TAG) with singleton {
        Retrofit.Builder()
            .client(
                instance<OkHttpClient>()
                    .newBuilder()
                    .addInterceptor(PhishNetAuthInterceptor(instance()))
                    .build()
            )
            .addConverterFactory(
                instance<Json>().asConverterFactory("application/json; charset=UTF8".toMediaType())
            )
            .baseUrl(instance<HttpUrl>(PHISH_NET_URL_TAG))
            .build()
    }

    bind<PhishNetService>() with singleton {
        instance<Retrofit>(PHISH_NET_RETROFIT_TAG).create(PhishNetService::class.java)
    }

    bind<PhishNetRepository>() with singleton {
        PhishNetRepository(instance())
    }

    bind<HttpUrl>(tag = PHISH_NET_URL_TAG) with singleton { PHISH_NET_API_URL }
}
