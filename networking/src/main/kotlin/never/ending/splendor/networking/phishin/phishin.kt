package never.ending.splendor.networking.phishin

suspend fun <T> retry(
    block: suspend () -> PhishinResponse<T>
): PhishinResponse<T> {
    repeat(2) {
        when (val result = block()) {
            is PhishinSuccess -> return result
        }
    }
    return block() // last attempt
}
