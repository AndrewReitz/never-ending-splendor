package nes.networking.phishnet

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.resultFrom
import nes.networking.phishnet.model.Review
import nes.networking.phishnet.model.SetList

class PhishNetRepository internal constructor(
    private val phishNetService: PhishNetService
) {
    suspend fun setlist(showDate: String): Result<List<SetList>, Exception> = resultFrom {
        phishNetService.setlist(showDate).response.data
    }

    suspend fun reviews(showId: String): Result<List<Review>, Exception> = resultFrom {
        phishNetService.reviews(showId).response.data
    }
}
