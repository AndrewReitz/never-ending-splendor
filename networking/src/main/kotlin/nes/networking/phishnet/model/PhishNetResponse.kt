package nes.networking.phishnet.model

import kotlinx.serialization.Serializable

@Serializable
data class PhishNetResponse<T>(
    val data: List<T>
)
