package nes.networking.phishnet

import com.google.common.truth.Truth.assertThat
import nes.networking.phishnet.PhishNetApiKey
import nes.networking.phishnet.PhishNetAuthInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.Test

class PhishNetAuthInterceptorTest {

    @Test
    fun `should add apikey query parameter`() {
        val apikey = "rescuesquad"
        val classUnderTest = PhishNetAuthInterceptor(PhishNetApiKey(apikey))

        val mockWebServer = MockWebServer()
        mockWebServer.start()
        mockWebServer.enqueue(MockResponse())

        val okHttpClient: OkHttpClient = OkHttpClient().newBuilder()
            .addInterceptor(classUnderTest).build()
        okHttpClient.newCall(Request.Builder().url(mockWebServer.url("/")).build()).execute()

        val request: RecordedRequest = mockWebServer.takeRequest()
        assertThat(request.requestUrl?.queryParameter("apikey")).isEqualTo(apikey)

        mockWebServer.shutdown()
    }
}
