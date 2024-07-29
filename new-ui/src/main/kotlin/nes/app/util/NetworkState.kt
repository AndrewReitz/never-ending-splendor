package nes.app.util

sealed interface NetworkState<out CONTENT, out ERROR> {
    data object Loading: NetworkState<Nothing, Nothing>
    data class Loaded<out C>(val value: C): NetworkState<C, Nothing>
    data class Error<E>(val error: E): NetworkState<Nothing, E>
}

fun <IN, OUT, E> NetworkState<IN, E>.map(transform: (IN) -> OUT): NetworkState<OUT, E> =
    when(this) {
        is NetworkState.Error -> this
        is NetworkState.Loaded -> NetworkState.Loaded(transform(value))
        NetworkState.Loading -> this as NetworkState<OUT, E>
    }

fun <IN, OUT, E> NetworkState<List<IN>, E>.mapCollection(transform: (IN) -> OUT): NetworkState<List<OUT>, E> =
    when(this) {
        is NetworkState.Error -> this
        is NetworkState.Loaded -> NetworkState.Loaded(value.map(transform))
        NetworkState.Loading -> this as NetworkState<List<OUT>, E>
    }
