package never.ending.splendor.networking.phishin

import never.ending.splendor.networking.phishin.model.Show
import never.ending.splendor.networking.phishin.model.SuccessfulResponse
import never.ending.splendor.networking.phishin.model.YearData
import retrofit2.http.GET
import retrofit2.http.Path

internal interface PhishinService {
    @GET("api/v1/years?include_show_counts=true")
    suspend fun years(): SuccessfulResponse<List<YearData>>

    @GET("api/v1/years/{year}")
    suspend fun shows(@Path("year") year: String): SuccessfulResponse<List<Show>>

    @GET("api/v1/shows/{id}")
    suspend fun show(@Path("id") showId: String): SuccessfulResponse<Show>
}
