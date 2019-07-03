package me.showang.respect.okhttp


import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import me.showang.respect.core.ApiSpec
import me.showang.respect.core.HttpMethod
import me.showang.respect.core.RequestError
import me.showang.respect.core.RequestExecutor
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit

open class OkhttpRequestExecutor(
        private val httpClient: OkHttpClient = OkHttpClient()
) : RequestExecutor {

    private val callMap: MutableMap<ApiSpec, Call> = mutableMapOf()

    @Throws(RequestError::class)
    override suspend fun request(api: ApiSpec): ByteArray = withContext(IO) {
        var response: Response? = null
        try {
            response = getResponse(api)
            if (response.isSuccessful) {
                response.body()?.bytes() ?: ByteArray(0)
            } else {
                throw Error("request unsuccessful")
            }
        } catch (e: Throwable) {
            try {
                throw RequestError(e, response?.code() ?: 0, response?.body()?.bytes())
            } catch (ex: Exception) { // Read error message when socket closed.
                throw RequestError(ex, response?.code() ?: 0, ex.message?.toByteArray())
            }
        }
    }

    override fun cancel(api: ApiSpec) {
        callMap[api]?.cancel()
        callMap.remove(api)
    }

    override fun cancelAll() {
        httpClient.dispatcher().cancelAll()
    }

    @Throws(Throwable::class)
    private fun getResponse(api: ApiSpec): Response {
        return clientWith(api).newCall(generateRequest(api)).run {
            callMap[api] = this
            execute()
        }
    }

    private fun clientWith(api: ApiSpec): OkHttpClient =
            with(httpClient.newBuilder()) {
                readTimeout(api.timeout, TimeUnit.MILLISECONDS)
                build()
            }

    private fun generateRequest(api: ApiSpec): Request =
            with(Request.Builder()) {
                headers(headers(api))
                when (api.httpMethod) {
                    HttpMethod.GET -> get()
                    HttpMethod.POST -> post(generateBody(api))
                    HttpMethod.PUT -> put(generateBody(api))
                    HttpMethod.DELETE -> delete(generateBody(api))
                }
                url(httpUrlWithQueries(api))
                build()
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
        headerBuilder.add("Content-Type", api.contentType)
        return headerBuilder.build()
    }

    private fun generateBody(api: ApiSpec): RequestBody {
        return RequestBody.create(MediaType.parse(api.contentType), api.body)
    }

}