package nes.networking.phishin.model

import kotlinx.serialization.Serializable

@Serializable
data class Venue(
    val name: String,
    val location: String
)
