package nes.networking

import com.jakewharton.byteunits.DecimalByteUnit.MEGABYTES
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class NetworkingModule {
    companion object {
        val DISK_CACHE_SIZE = MEGABYTES.toBytes(100).toInt()
        val PHISHIN_API_URL: HttpUrl = requireNotNull("https://phish.in/".toHttpUrlOrNull())
    }
}