package never.ending.splendor.networking

import com.jakewharton.byteunits.DecimalByteUnit.MEGABYTES
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import never.ending.splendor.networking.moshi.HttpUrlAdapter
import never.ending.splendor.networking.phishin.PhishinAuthInterceptor
import never.ending.splendor.networking.phishin.PhishinRepository
import never.ending.splendor.networking.phishin.PhishinService
import never.ending.splendor.networking.phishnet.PhishNetAuthInterceptor
import okhttp3.Cache
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.setBinding
import org.kodein.di.singleton
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.Date

/**
 * Tag for networking module to provide the cache directory for http client
 */
const val CACHE_DIR_TAG = "CacheDir"

const val PHISHIN_RETROFIT_TAG = "Phishin"
const val PHISH_NET_RETROFIT_TAG = "p[hish.net"

private val PHISHIN_API_URL: HttpUrl = requireNotNull("https://phish.in/".toHttpUrlOrNull())
private val PHISH_NET_API_URL: HttpUrl = requireNotNull("https://api.phish.net/v3/".toHttpUrlOrNull())
private val DISK_CACHE_SIZE = MEGABYTES.toBytes(50).toInt()

// todo break into two smaller modules

val networkingModule = DI.Module(name = "NetworkingModule") {

    bind<PhishinService>() with singleton {
        instance<Retrofit>(tag = PHISHIN_RETROFIT_TAG).create(PhishinService::class.java)
    }

    bind<PhishinRepository>() with singleton { PhishinRepository(instance()) }

    bind<Retrofit>(tag = PHISHIN_RETROFIT_TAG) with singleton {
        Retrofit.Builder()
            .client(
                instance<OkHttpClient>()
                    .newBuilder()
                    .addInterceptor(PhishinAuthInterceptor(instance()))
                    .build()
            )
            .addConverterFactory(MoshiConverterFactory.create(instance()))
            .baseUrl(PHISHIN_API_URL)
            .build()
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

    bind<Moshi>() with singleton {
        Moshi.Builder()
            .add(Date::class.java, Rfc3339DateJsonAdapter())
            .add(HttpUrlAdapter)
            .build()
    }

    bind<OkHttpClient>() with singleton {
        OkHttpClient.Builder()
            .cache(Cache(File(instance<File>(tag = CACHE_DIR_TAG), "http"), DISK_CACHE_SIZE.toLong()))
            .apply { instance<Set<Interceptor>>().forEach { addInterceptor(it) } }
            .build()
    }

    bind() from setBinding<Interceptor>()
}
