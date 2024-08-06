package nes.app.util

import android.os.Bundle
import nes.networking.phishin.model.Show

fun Show.toMetadataExtras(): Bundle = Bundle().apply {
    putLong("showId", id)
    putString("venueName", venue_name)
}

fun Bundle.toShowInfo(): Pair<Long, String> = getLong("showId") to getString("venueName", "")