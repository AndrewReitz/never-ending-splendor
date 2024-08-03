package nes.networking.phishin.model

import kotlinx.serialization.Serializable
import nes.networking.serializers.HttpUrlSerializer
import okhttp3.HttpUrl
import java.util.Locale
import java.util.concurrent.TimeUnit

@Serializable
data class Track(
    val id: Long,
    val title: String,
    val mp3: String,
    val duration: Long
) {
    val formatedDuration: String get() {
        return String.format(
            locale = Locale.US,
            "%d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(duration),
            TimeUnit.MILLISECONDS.toSeconds(duration) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))
        )
    }
}
