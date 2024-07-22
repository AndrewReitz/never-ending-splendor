package nes.networking.phishin.model

import kotlinx.serialization.Serializable

@Serializable
data class SuccessfulResponse<T>(
    val total_entries: Int,
    val total_pages: Int,
    val page: Int,
    val data: T
)
