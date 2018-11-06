package me.showang.respect.okhttp


import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import me.showang.respect.core.ApiSpec
import me.showang.respect.core.HttpMethod
import me.showang.respect.core.RequestExecutor
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit

open class OkhttpRequestExecutor(
        private val httpClient: OkHttpClient = OkHttpClient()
) : RequestExecutor {

    private val callMap: MutableMap<ApiSpec, Call> = mutableMapOf()

    override suspend fun request(api: ApiSpec): ByteArray = withContext(IO) {
        getResponse(api).body()?.bytes() ?: ByteArray(0)
    }

    override fun cancel(api: ApiSpec) {
        callMap[api]?.cancel()
        callMap.remove(api)
    }

    override fun cancelAll() {
        httpClient.dispatcher().cancelAll()
    }

    @Throws(IOException::class)
    private fun getResponse(api: ApiSpec): Response {
        val request = generateRequest(api)
        val call = httpClient.newBuilder()
                .readTimeout(api.timeout, TimeUnit.MILLISECONDS)
                .build().newCall(request)
        callMap[api] = call
        return call.execute()
    }

    private fun generateRequest(api: ApiSpec): Request {
        val builder = Request.Builder()
                .headers(headers(api))
        return when (api.httpMethod) {
            HttpMethod.GET -> builder.get()
            HttpMethod.POST -> builder.post(generateBody(api))
            HttpMethod.PUT -> builder.put(generateBody(api))
            HttpMethod.DELETE -> builder.delete(generateBody(api))
        }.url(httpUrlWithQueries(api)).build()
    }

    private fun httpUrlWithQueries(api: ApiSpec): HttpUrl {
        val urlBuilder = httpUrl(api).newBuilder()
        api.urlQueries.forEach { (key, value) ->
            urlBuilder.addQueryParameter(key, value)
        }
        return urlBuilder.build()
    }

    private fun httpUrl(api: ApiSpec): HttpUrl {
        return HttpUrl.parse(api.url) ?: throw RuntimeException("url is not available")
    }

    private fun headers(api: ApiSpec): Headers {
        val headerBuilder = Headers.Builder()
        api.headers.forEach { (key, value) ->
            headerBuilder.add(key, value)
        }
        return headerBuilder.build()
    }

    private fun generateBody(api: ApiSpec): RequestBody {
        return RequestBody.create(MediaType.parse(api.contentType), api.body)
    }

}