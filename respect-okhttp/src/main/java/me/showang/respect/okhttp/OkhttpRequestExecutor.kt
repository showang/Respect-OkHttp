package me.showang.respect.okhttp


import me.showang.respect.core.ApiSpec
import me.showang.respect.core.HttpMethod
import me.showang.respect.core.RequestExecutor
import me.showang.respect.core.async.AndroidAsyncManager
import me.showang.respect.core.async.AsyncManager
import me.showang.respect.core.async.FakeAsyncManager
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit

open class OkhttpRequestExecutor(
        private val httpClient: OkHttpClient = OkHttpClient(),
        syncMode: Boolean = false
) : RequestExecutor {

    private val callMap: MutableMap<Any, Call> = mutableMapOf()
    override val asyncManager: AsyncManager = if (syncMode) FakeAsyncManager() else AndroidAsyncManager()

    override fun request(api: ApiSpec, tag: Any, failCallback: (error: Error) -> Unit, completeCallback: (response: ByteArray) -> Unit) {
        asyncManager.start(background = {
            var error: Error? = null
            var result: ByteArray? = null
            try {
                val response = getResponse(api, tag)
                result = response.body()?.bytes() ?: ByteArray(0)
            } catch (e: Error) {
                error = e
            }
            when {
                error != null -> failCallback(error)
                result != null -> completeCallback(result)
                else -> failCallback(Error("Unknown error"))
            }
            true
        }, uiThread = {})
    }

    override fun cancel(tag: Any) {
        callMap[tag]?.cancel()
        callMap.remove(tag)
    }

    override fun cancelAll() {
        httpClient.dispatcher().cancelAll()
    }

    @Throws(IOException::class)
    private fun getResponse(api: ApiSpec, tag: Any): Response {
        val request = generateRequest(api, tag)
        val call = httpClient.newBuilder()
                .readTimeout(api.timeout, TimeUnit.MILLISECONDS)
                .build().newCall(request)
        callMap[tag] = call
        return call.execute()
    }

    private fun generateRequest(api: ApiSpec, tag: Any): Request {
        val builder = Request.Builder()
                .headers(headers(api))
                .tag(tag)
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