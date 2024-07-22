package nes.networking.phishin

import nes.networking.phishin.model.Show
import nes.networking.phishin.model.SuccessfulResponse
import nes.networking.phishin.model.YearData
import retrofit2.http.GET
import retrofit2.http.Path

interface PhishInService {

    @GET("api/v1/years?include_show_counts=true")
    suspend fun years(): SuccessfulResponse<List<YearData>>

    @GET("api/v1/years/{year}")
    suspend fun shows(@Path("year") year: String): SuccessfulResponse<List<Show>>

    @GET("api/v1/shows/{id}")
    suspend fun show(@Path("id") showId: String): SuccessfulResponse<Show>
}
