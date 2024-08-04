@file:Suppress("UNCHECKED_CAST")

package nes.app.util

sealed interface LCE<out CONTENT, out ERROR> {
    data object Loading: LCE<Nothing, Nothing>
    data class Loaded<out C>(val value: C): LCE<C, Nothing>
    data class Error<E>(val userDisplayedMessage: String, val error: E): LCE<Nothing, E>
}

fun <IN, OUT, E> LCE<IN, E>.map(transform: (IN) -> OUT): LCE<OUT, E> =
    when(this) {
        is LCE.Error -> this
        is LCE.Loaded -> LCE.Loaded(transform(value))
        LCE.Loading -> this as LCE<OUT, E>
    }

fun <IN, OUT, E> LCE<List<IN>, E>.mapCollection(transform: (IN) -> OUT): LCE<List<OUT>, E> =
    when(this) {
        is LCE.Error -> this
        is LCE.Loaded -> LCE.Loaded(value.map(transform))
        LCE.Loading -> this as LCE<List<OUT>, E>
    }
