package nes.networking

import com.jakewharton.byteunits.DecimalByteUnit.MEGABYTES
import kotlinx.serialization.json.Json
import nes.networking.phishin.phishInModule
import nes.networking.phishnet.phishNetModule
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.bindSet
import org.kodein.di.instance
import org.kodein.di.singleton
import java.io.File
import java.util.Date

/**
 * Tag for networking module to provide the cache directory for http client
 */
const val CACHE_DIR_TAG = "CacheDir"

private val DISK_CACHE_SIZE = MEGABYTES.toBytes(100).toInt()

val networkingModule = DI.Module(name = "NetworkingModule") {

    import(phishInModule)
    import(phishNetModule)

    bind<Json>() with singleton {
        Json {
            ignoreUnknownKeys = true
        }
    }

    bind<OkHttpClient>() with singleton {
        OkHttpClient.Builder()
            .cache(Cache(File(instance<File>(tag = CACHE_DIR_TAG), "http"), DISK_CACHE_SIZE.toLong()))
            .apply { instance<Set<Interceptor>>().forEach { addInterceptor(it) } }
            .build()
    }

    bindSet<Interceptor>()
}
