package nes.networking.phishnet

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

internal class PhishNetAuthInterceptor(
    private val apiKey: PhishNetApiKey
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original: Request = chain.request()

        val urlWithAuthKey = original.url.newBuilder()
            .addQueryParameter("apikey", apiKey.apiKey)
            .build()

        val builder = original.newBuilder()
            .url(urlWithAuthKey)

        val request: Request = builder.build()
        return chain.proceed(request)
    }
}
