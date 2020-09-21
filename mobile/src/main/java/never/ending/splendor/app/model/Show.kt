package never.ending.splendor.app.model

import java.text.SimpleDateFormat
import java.util.*

class Show {
    var id: Long = 0
    var date: Date? = null
    var duration: Long = 0
    var isSbd = false
    var tourId: Long = 0
    var venueName: String? = null
    var location: String? = null
    var taperNotes: String? = null
    val tracks: MutableList<Track> = mutableListOf()
    val dateSimple: String
        get() {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd")
            return dateFormat.format(date)
        }
}
