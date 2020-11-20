package nes.networking.phishnet.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SetList(
    val showid: Long,
    val showdate: String,
    val short_date: String,
    val long_date: String,
    val relative_date: String,
    val url: String,
    val gapchart: String,
    val artist: String,
    val artistid: String,
    val venueid: String,
    val venue: String,
    val location: String,
    val setlistdata: String,
    val setlistnotes: String,
    val rating: String
)
