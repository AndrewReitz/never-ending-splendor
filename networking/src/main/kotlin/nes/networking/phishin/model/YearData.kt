package nes.networking.phishin.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YearData(
    val date: String,
    val show_count: Int
)
