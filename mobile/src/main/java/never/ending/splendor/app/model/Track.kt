package never.ending.splendor.app.model

import java.util.*
import java.util.concurrent.TimeUnit

class Track(var id: Long, var title: String, trackUrl: String) {
    var duration: Long = 0
    var set: String? = null
    var setName: String? = null
    var url: String
    private val mSongIds: ArrayList<Int>? = null
    val durationString: String
        get() = String.format(Locale.ROOT, "%d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(duration),
                TimeUnit.MILLISECONDS.toSeconds(duration) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))
        )

    init {
        val parts = trackUrl.split("https://".toRegex()).toTypedArray()
        url = "http://" + parts[1]
    }
}
