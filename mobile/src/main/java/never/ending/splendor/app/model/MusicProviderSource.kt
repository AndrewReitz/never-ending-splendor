package never.ending.splendor.app.model

import android.support.v4.media.MediaMetadataCompat
import never.ending.splendor.networking.phishin.model.YearData

interface MusicProviderSource {
    suspend fun years(): List<YearData>
    suspend fun showsInYear(year: String): List<MediaMetadataCompat>
    suspend fun tracksInShow(showId: String): List<MediaMetadataCompat>

    companion object {
        const val CUSTOM_METADATA_TRACK_SOURCE = "__SOURCE__"
    }
}
