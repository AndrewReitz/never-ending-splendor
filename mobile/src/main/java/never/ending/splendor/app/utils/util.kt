package never.ending.splendor.app.utils

import java.util.Collections

// junk drawer for code.

/** Returns an immutable copy of this. */
fun <T> List<T>.toImmutableList(): List<T> = Collections.unmodifiableList(toList())
