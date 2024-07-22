package never.ending.splendor.app.model

import android.support.v4.media.MediaMetadataCompat
import com.squareup.picasso.Picasso
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import nes.networking.phishin.PhishInRepository
import nes.networking.phishin.model.YearData
import nes.networking.retry
import never.ending.splendor.app.utils.Images
import never.ending.splendor.app.utils.toSimpleFormat
import timber.log.Timber

class PhishProviderSource(
    private val phishinRepository: PhishInRepository,
    private val images: Images
) : MusicProviderSource {

    override suspend fun years(): List<YearData> =
        when (val result = retry { phishinRepository.years() }) {
            is Success -> result.value
            is Failure -> {
                Timber.e(result.reason, "There was an error loading years from phishin api")
                emptyList()
            }
            else -> error("Getting AS to shut up. This shouldn't happen")
        }

    override suspend fun showsInYear(year: String): List<MediaMetadataCompat> {
        return when (val result = retry { phishinRepository.shows(year) }) {
            is Success -> result.value.map { show ->
                MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, show.id.toString())
                    .putString(MediaMetadataCompat.METADATA_KEY_DATE, show.date.toSimpleFormat())
                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, show.venue_name)
                    // we're using 'Author' here for taper notes
                    .putString(
                        MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE,
                        show.venue.location
                    )
                    .putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, show.taper_notes)
                    .build()
            }.asReversed()
            is Failure -> {
                Timber.e(
                    result.reason,
                    "There was an error loading shows in %s from phishin api",
                    year
                )
                emptyList()
            }
            else -> error("Getting AS to shut up. This shouldn't happen")
        }
    }

    override suspend fun tracksInShow(showId: String): List<MediaMetadataCompat> {
        return when (val result = retry { phishinRepository.show(showId) }) {
            is Success -> {
                result.value.let { show ->
                    result.value.tracks.map { track ->
                        // Adding the music source to the MediaMetadata (and consequently using it in the
                        // mediaSession.setMetadata) is not a good idea for a real world music app, because
                        // the session metadata can be accessed by notification listeners. This is done in this
                        // sample for convenience only.
                        MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, track.id.toString())
                            .putString(
                                MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE,
                                track.mp3.toString()
                            )
                            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.duration)
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
                            // pretty hokey, but we're overloading these fields so we can save venue and location, and showId
                            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, show.venue_name)
                            .putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, show.taper_notes)
                            .putString(MediaMetadataCompat.METADATA_KEY_COMPILATION, show.id.toString())
                            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, track.title)
                            .putString(
                                MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE,
                                track.formatedDuration
                            )
                            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.duration)
                            .putString(
                                MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION,
                                show.date.toSimpleFormat()
                            )
                            .putString(
                                MediaMetadataCompat.METADATA_KEY_ARTIST,
                                "Phish"
                            )
                            .putString(
                                MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                                images.randomImageUrl
                            )
                            .build()
                    }
                }
            }
            is Failure -> {
                Timber.e(
                    result.reason,
                    "There was an error loading show %s from phishin api",
                    showId
                )
                emptyList()
            }
            else -> error("Getting AS to shut up. This shouldn't happen")
        }
    }
}
