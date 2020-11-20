package nes.networking.phishnet.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Review(
    val reviewid: Int,
    val posted_date: String,
    val reviewlink: String,
    val uid: Int,
    val username: String,
    val avatar: String,
    val link: String,
    val artist: String,
    val showid: Int,
    val reviewtext: String,
    val score: Int,
    val venue: String,
    val city: String,
    val state: String,
    val country: String
)
