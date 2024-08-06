package nes.networking

import okio.BufferedSource
import okio.buffer
import okio.source

val showJson get() = loadFile("phishin_show.json")
val showsJson get() = loadFile("phishin_shows.json")
val yearsJson get() = loadFile("phishin_years.json")
val reviews get() = loadFile("phishnet_reviews.json")
val setlist get() = loadFile("phishnet_setlist.json")

private fun loadFile(fileName: String): BufferedSource =
    requireNotNull(ClassLoader.getSystemResourceAsStream(fileName))
        .source()
        .buffer()
