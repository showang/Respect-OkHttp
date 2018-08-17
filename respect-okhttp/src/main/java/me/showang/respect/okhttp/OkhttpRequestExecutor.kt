package me.showang.respect.okhttp


import me.showang.respect.core.HttpMethod
import me.showang.respect.core.RequestExecutor
import me.showang.respect.core.RespectApi
import me.showang.respect.core.async.AndroidAsyncManager
import me.showang.respect.core.async.AsyncManager
import me.showang.respect.core.async.SyncManager
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit

open class OkhttpRequestExecutor(
        private val httpClient: OkHttpClient = OkHttpClient(),
        syncMode: Boolean = false
) : RequestExecutor {

    private val callMap: MutableMap<Any, Call> = mutableMapOf()
    override val asyncManager: AsyncManager = if (syncMode) SyncManager() else AndroidAsyncManager()

    override fun request(api: RespectApi, tag: Any, failCallback: (error: Error) -> Unit, completeCallback: (response: ByteArray) -> Unit) {
        asyncManager.start(background = {
            var error: Error? = null
            var result: ByteArray? = null
            try {
                val response = getResponse(api, tag)
                result = response.body()?.bytes() ?: ByteArray(0)
            } catch (e: Error) {
                error = e
            }
            Pair(result, error)
        }, uiThread = {
            val result = it.first
            val error = it.second
            callMap.remove(tag)
            when {
                error != null -> failCallback(error)
                result != null -> completeCallback(result)
                else -> failCallback(Error("Unknown error"))
            }
        })
    }

    override fun cancel(tag: Any) {
        callMap[tag]?.cancel()
        callMap.remove(tag)
    }

    override fun cancelAll() {
        httpClient.dispatcher().cancelAll()
    }

    @Throws(IOException::class)
    private fun getResponse(api: RespectApi, tag: Any): Response {
        val request = generateRequest(api, tag)
        val call = httpClient.newBuilder()
                .readTimeout(api.timeout, TimeUnit.MILLISECONDS)
                .build().newCall(request)
        callMap[tag] = call
        return call.execute()
    }

    private fun generateRequest(api: RespectApi, tag: Any): Request {
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

    private fun httpUrlWithQueries(api: RespectApi): HttpUrl {
        val urlBuilder = httpUrl(api).newBuilder()
        api.urlQueries.forEach { (key, value) ->
            urlBuilder.addQueryParameter(key, value)
        }
        return urlBuilder.build()
    }

    private fun httpUrl(api: RespectApi): HttpUrl {
        return HttpUrl.parse(api.url) ?: throw RuntimeException("url is not available")
    }

    private fun headers(api: RespectApi): Headers {
        val headerBuilder = Headers.Builder()
        api.headers.forEach { (key, value) ->
            headerBuilder.add(key, value)
        }
        return headerBuilder.build()
    }

    private fun generateBody(api: RespectApi): RequestBody {
        return RequestBody.create(MediaType.parse(api.contentType), api.body)
    }

}