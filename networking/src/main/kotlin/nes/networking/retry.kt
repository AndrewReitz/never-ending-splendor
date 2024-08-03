package nes.networking

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success

// TODO replace w/ Arrow?
suspend fun <T> retry(
    block: suspend () -> Result<T, Exception>
): Result<T, Exception> {
    repeat(2) {
        when (val result = block()) {
            is Success -> return@retry result
            is Failure -> Unit // no-op
        }
    }
    return block() // last attempt
}
