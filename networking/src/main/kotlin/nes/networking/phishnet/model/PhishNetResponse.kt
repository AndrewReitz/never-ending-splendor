package nes.networking.phishnet.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PhishNetResponse<T>(
    val data: List<T>
)
