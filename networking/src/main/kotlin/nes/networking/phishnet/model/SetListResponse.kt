package nes.networking.phishnet.model

import kotlinx.serialization.Serializable

@Serializable
data class SetListResponse(
    val showid: Long,
    val showdate: String,
    val venueid: String,
    val venue: String,
    val city: String,
    val setlistnotes: String,
    val song: String,
)
