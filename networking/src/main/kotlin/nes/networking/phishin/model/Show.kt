package nes.networking.phishin.model

import com.squareup.moshi.JsonClass
import java.util.Date

@JsonClass(generateAdapter = true)
data class Show(
    val id: String,
    val date: Date,
    val venue_name: String,
    val taper_notes: String?,
    val venue: Venue,
    val tracks: List<Track>
)
