package nes.networking.phishin

import com.google.common.truth.Truth.assertThat
import nes.networking.phishin.PhishinApiKey
import nes.networking.phishin.PhishinAuthInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import kotlin.test.Test

class PhishinAuthInterceptorTest {

    @Test
    fun `should add phishin auth header`() {
        val classUnderTest = PhishinAuthInterceptor(PhishinApiKey("rubywaves"))

        val mockWebServer = MockWebServer()
        mockWebServer.start()
        mockWebServer.enqueue(MockResponse())

        val okHttpClient: OkHttpClient = OkHttpClient().newBuilder()
            .addInterceptor(classUnderTest).build()
        okHttpClient.newCall(Request.Builder().url(mockWebServer.url("/")).build()).execute()

        val request: RecordedRequest = mockWebServer.takeRequest()
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer rubywaves")

        mockWebServer.shutdown()
    }
}
