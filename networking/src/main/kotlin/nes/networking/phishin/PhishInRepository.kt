package nes.networking.phishin

import nes.networking.phishin.model.Show
import nes.networking.phishin.model.YearData
import kotlin.Exception
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.resultFrom

class PhishInRepository internal constructor(
    private val phishInService: PhishInService
) {
    suspend fun years(): Result<List<YearData>, Exception> = resultFrom {
        phishInService.years().data
    }

    suspend fun shows(year: String): Result<List<Show>, Exception> = resultFrom {
        phishInService.shows(year).data
    }

    suspend fun show(showId: String): Result<Show, Exception> = resultFrom {
        phishInService.show(showId).data
    }
}
