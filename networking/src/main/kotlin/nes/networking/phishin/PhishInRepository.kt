package nes.networking.phishin

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.resultFrom
import nes.networking.phishin.model.Show
import nes.networking.phishin.model.YearData
import kotlin.Exception

class PhishInRepository(
    private val phishInService: PhishInService
) {
    suspend fun years(): Result<List<YearData>, Exception> = resultFrom {
        phishInService.years().data.reversed()
    }

    suspend fun shows(year: String): Result<List<Show>, Exception> = resultFrom {
        phishInService.shows(year).data
    }

    suspend fun show(showId: String): Result<Show, Exception> = resultFrom {
        phishInService.show(showId).data
    }
}
