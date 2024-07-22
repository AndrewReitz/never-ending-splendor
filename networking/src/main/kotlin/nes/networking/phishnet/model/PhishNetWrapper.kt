package nes.networking.phishnet.model

import kotlinx.serialization.Serializable

@Serializable
data class PhishNetWrapper<T>(
    val error_message: String = "",
    val data: List<T>
)
