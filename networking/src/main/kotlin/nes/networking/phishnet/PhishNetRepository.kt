package nes.networking.phishnet

import arrow.core.Either
import arrow.core.raise.either
import kotlinx.coroutines.withTimeout
import nes.networking.phishnet.model.Review
import nes.networking.phishnet.model.SetList
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class PhishNetRepository @Inject constructor(
    private val phishNetService: PhishNetService
) {

    companion object {
        private const val TIME_OUT = 1500
    }

    suspend fun setlist(showDate: String): Either<Throwable, SetList> = Either.catch {
        withTimeout(TIME_OUT.milliseconds) {
            val data = phishNetService.setlist(showDate).data
            val firstSong = data[0]
            val songs = data.map { it.song }

            SetList(
                showid = firstSong.showid,
                showdate = firstSong.showdate,
                venue = firstSong.venue,
                city = firstSong.city,
                setlistnotes = firstSong.setlistnotes,
                songs = songs
            )
        }
    }

    suspend fun reviews(showId: String): Either<Throwable, List<Review>> = Either.catch {
        withTimeout(TIME_OUT.milliseconds) {
            phishNetService.reviews(showId).data
        }
    }
}
