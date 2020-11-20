package nes.networking.phishnet

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

const val PHISH_NET_RETROFIT_TAG = "phish.net"

private val PHISH_NET_API_URL: HttpUrl = requireNotNull("https://api.phish.net/v3/".toHttpUrlOrNull())

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
            .addConverterFactory(MoshiConverterFactory.create(instance()))
            .baseUrl(PHISH_NET_API_URL)
            .build()
    }

    bind<PhishNetService>() with singleton {
        instance<Retrofit>(PHISH_NET_RETROFIT_TAG).create(PhishNetService::class.java)
    }

    bind<PhishNetRepository>() with singleton {
        PhishNetRepository(instance())
    }
}
