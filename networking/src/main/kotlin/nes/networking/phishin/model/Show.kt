package nes.networking.phishin.model

import kotlinx.serialization.Serializable
import nes.networking.adpters.DateJsonAdapter
import java.util.Date

@Serializable
data class Show(
    val id: Int,
    @Serializable(with = DateJsonAdapter::class)
    val date: Date,
    val venue_name: String,
    val taper_notes: String?,
    val venue: Venue,
    val tracks: List<Track>
)
