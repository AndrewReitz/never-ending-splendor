package nes.networking.phishin.model

import kotlinx.serialization.Serializable
import nes.networking.serializers.DateJsonSerializer
import java.util.Date

@Serializable
data class Show(
    val id: Long,
    @Serializable(with = DateJsonSerializer::class)
    val date: Date,
    val venue_name: String,
    val taper_notes: String?,
    val venue: Venue,
    val tracks: List<Track>
)
