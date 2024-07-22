package nes.networking.phishin.model

import kotlinx.serialization.Serializable

@Serializable
data class YearData(
    val date: String,
    val show_count: Int
)
