package nes.networking.phishnet.model

data class SetList(
    val showid: Long,
    val showdate: String,
    val venue: String,
    val city: String,
    val setlistnotes: String,
    val songs: List<String>
)
