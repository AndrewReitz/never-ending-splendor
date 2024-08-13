package nes.networking.phishin

import arrow.core.Either
import kotlinx.coroutines.withTimeout
import nes.networking.phishin.model.Show
import nes.networking.phishin.model.YearData
import kotlin.time.Duration.Companion.milliseconds

class PhishInRepository(
    private val phishInService: PhishInService
) {
    companion object {
        private const val TIME_OUT = 1500
    }

    suspend fun years(): Either<Throwable, List<YearData>> = Either.catch {
        withTimeout(TIME_OUT.milliseconds) {
            phishInService.years().data.reversed()
        }
    }

    suspend fun shows(year: String): Either<Throwable, List<Show>> = Either.catch {
        withTimeout(TIME_OUT.milliseconds) {
            phishInService.shows(year).data
        }
    }

    suspend fun show(showId: String): Either<Throwable, Show> = Either.catch {
        withTimeout(TIME_OUT.milliseconds) {
            phishInService.show(showId).data
        }
    }
}
