package never.ending.splendor.networking.phishnet

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

interface PhishNetService {
    @GET("setlists/get")
    suspend fun setlist(@Query("showdate") showDate: String): PhishNetWrapper<SetList>

    @GET("reviews/query")
    suspend fun reviews(@Query("showid") showid: String): PhishNetWrapper<Review>
}

@JsonClass(generateAdapter = true)
data class PhishNetWrapper<T>(
    val error_message: String? = null,
    val response: PhishNetResponse<T>
)

@JsonClass(generateAdapter = true)
data class PhishNetResponse<T>(
    val data: List<T>
)

@JsonClass(generateAdapter = true)
data class SetList(
    val showid: Long,
    val showdate: String,
    val short_date: String,
    val long_date: String,
    val relative_date: String,
    val url: String,
    val gapchart: String,
    val artist: String,
    val artistid: String,
    val venueid: String,
    val venue: String,
    val location: String,
    val setlistdata: String,
    val setlistnotes: String,
    val rating: String
)

@JsonClass(generateAdapter = true)
data class Review(
    val reviewid: Int,
    val posted_date: String,
    val reviewlink: String,
    val uid: Int,
    val username: String,
    val avatar: String,
    val link: String,
    val artist: String,
    val showid: Int,
    val reviewtext: String,
    val score: Int,
    val venue: String,
    val city: String,
    val state: String,
    val country: String
)
