package nes.app

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import nes.app.ui.player.PlayerState
import nes.app.util.LCE
import nes.networking.phishin.model.Show
import nes.networking.phishin.model.Track
import nes.networking.phishin.model.Venue
import nes.networking.phishin.model.YearData
import java.time.Instant
import java.util.Date
import kotlin.time.Duration.Companion.minutes

val mediaItem = MediaItem.Builder()
    .setUri("https://phish.in/audio/000/032/562/32562.mp3")
    .setMediaId("https://phish.in/audio/000/032/562/32562.mp3")
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle("Free")
            .setAlbumTitle("Lake Tahoe Outdoor Arena at Harveys")
            .build()
    )
    .build()

val noShowPlayerState = PlayerState.NoMedia

val showingPlayerState = PlayerState.MediaLoaded(
    isPlaying = true,
    formatedDurationTime = "13:37",
    formatedElapsedTime = "1:23",
    duration = 7.minutes.inWholeMilliseconds,
    currentPosition = 1.minutes.inWholeMilliseconds,
    showId = 1000,
    venueName = "Lake Tahoe Outdoor Arena at Harveys",
    artworkUri = "https://i.imgur.com/qhqUJWh.jpg".toUri(),
    albumTitle = "10/10/2024 : Lake Tahoe Outdoor Arena at Harveys",
    title = "Free",
    mediaId = "https://phish.in/audio/000/032/562/32562.mp3"
)

val yearsContent = LCE.Content(
    value = listOf(
        YearData(
            date = "1983-1987",
            show_count = 34
        ),
        YearData(
            date = "1988",
            show_count = 44
        ),
        YearData(
            date = "1989",
            show_count = 63
        ),

        YearData(
            date = "1990",
            show_count = 92
        ),

        YearData(
            date = "1991",
            show_count = 116
        ),

        YearData(
            date = "1992",
            show_count = 108
        ),

        YearData(
            date = "1993",
            show_count = 109
        ),

        YearData(
            date = "1994",
            show_count = 124
        ),

        YearData(
            date = "1995",
            show_count = 82
        ),

        YearData(
            date = "1996",
            show_count = 71
        ),

        YearData(
            date = "1997",
            show_count = 81
        ),

        YearData(
            date = "1998",
            show_count = 70
        ),
    )
)

val show = Show(
    id = 1941L,
    date = Date.from(Instant.parse("2018-07-17T00:00:00.00Z")),
    venue_name = "Lake Tahoe Outdoor Arena at Harveys",
    taper_notes = null,
    venue = Venue(
        name = "Lake Tahoe Outdoor Arena at Harveys",
        location = "Stateline, NV"
    ),
    tracks = listOf(
        Track(
            id = 32562,
            title = "Free",
            mp3 = "https://phish.in/audio/000/032/562/32562.mp3",
            duration = 526655
        ),
        Track(
            id = 32562,
            title = "Free",
            mp3 = "https://phish.in/audio/000/032/562/32562.mp3",
            duration = 526655
        ),
        Track(
            id = 32562,
            title = "Free",
            mp3 = "https://phish.in/audio/000/032/562/32562.mp3",
            duration = 526655
        ),
        Track(
            id = 32562,
            title = "Free",
            mp3 = "https://phish.in/audio/000/032/562/32562.mp3",
            duration = 526655
        ),
        Track(
            id = 32562,
            title = "Free",
            mp3 = "https://phish.in/audio/000/032/562/32562.mp3",
            duration = 526655
        ),
        Track(
            id = 32562,
            title = "Free",
            mp3 = "https://phish.in/audio/000/032/562/32562.mp3",
            duration = 526655
        ),
        Track(
            id = 32562,
            title = "Free",
            mp3 = "https://phish.in/audio/000/032/562/32562.mp3",
            duration = 526655
        )
    )
)

val showListContent = LCE.Content(
    List(10) {
        show
    }
)

val showContent = LCE.Content(show)