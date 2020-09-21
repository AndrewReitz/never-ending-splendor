package never.ending.splendor.app.model

import org.json.JSONObject
import timber.log.Timber
import java.text.SimpleDateFormat

object ParseUtils {
    fun parseShow(json: JSONObject): Show? = try {
        val data = requireNotNull(json.getJSONObject("data"))
        parseShowData(data)
    } catch (e: Exception) {
        Timber.d("failed to parse show!")
        null
    }

    private fun parseShowData(data: JSONObject): Show? = try {
        val show = Show()
        val showId = data.getLong("id")
        show.id = showId
        val dateString = data.getString("date") //formatted as YYYY-MM-DD
        val date = SimpleDateFormat("yyyy-MM-dd").parse(dateString)
        show.date = date
        val venue = data.optJSONObject("venue")
        if (venue != null) {
            //full show data contains a venue object
            val venueName = venue.getString("name")
            show.venueName = venueName
            val location = venue.getString("location")
            show.location = location
        } else {
            val venueName = data.getString("venue_name")
            show.venueName = venueName
            val location = data.getString("location")
            show.location = location
        }

        //parse 'tracks' if available (if this is more that "simple data")
        val tracks = data.optJSONArray("tracks")
        if (tracks != null) {
            for (i in 0 until tracks.length()) {
                val jsonTrack = tracks.getJSONObject(i)
                Timber.d(jsonTrack.toString())
                val id = jsonTrack.getInt("id")
                val title = jsonTrack.getString("title")
                val url = jsonTrack.getString("mp3")
                val duration = jsonTrack.getLong("duration")
                val track = Track(id.toLong(), title, url)
                track.duration = duration
                show.tracks.add(track)
            }
        }
        val taperNotes = data.getString("taper_notes")
        show.taperNotes = taperNotes
        val sbd = data.getBoolean("sbd")
        show.isSbd = sbd
        show
    } catch (e: Exception) {
        Timber.d("failed to parse show!")
        null
    }
}
