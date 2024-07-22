package nes.networking.phishnet.model

import kotlinx.serialization.Serializable

@Serializable
data class Review(
    val reviewid: Int,
    val uid: Int,
    val username: String,
    val showid: Int,
    val score: Int,
    val venue: String,
    val city: String,
    val state: String,
    val country: String,
    val review_text: String,
    val posted_at: String
)
