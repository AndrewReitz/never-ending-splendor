package nes.networking.phishnet

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.resultFrom
import nes.networking.phishnet.model.Review
import nes.networking.phishnet.model.SetList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhishNetRepository @Inject constructor(
    private val phishNetService: PhishNetService
) {
    suspend fun setlist(showDate: String): Result<SetList, Exception> = resultFrom {
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

    suspend fun reviews(showId: String): Result<List<Review>, Exception> = resultFrom {
        phishNetService.reviews(showId).data
    }
}
