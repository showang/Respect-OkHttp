package me.showang.respect.okhttp


import kotlinx.coroutines.suspendCancellableCoroutine
import me.showang.respect.core.ApiSpec
import me.showang.respect.core.Headers.CONTENT_TYPE
import me.showang.respect.core.HttpMethod
import me.showang.respect.core.RequestExecutor
import me.showang.respect.core.error.RequestException
import okhttp3.*
import okhttp3.Headers.Companion.toHeaders
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

open class OkhttpRequestExecutor(
    private val httpClient: OkHttpClient = OkHttpClient()
) : RequestExecutor {

    private val callMap: MutableMap<ApiSpec, Call> = mutableMapOf()

    @Throws(RequestException::class)
    override suspend fun submit(api: ApiSpec): InputStream =
        suspendCancellableCoroutine { cancellable ->
            cancellable.invokeOnCancellation {
                cancel(api)
            }
            var response: Response? = null
            runCatching {
                getResponse(api).apply {
                    response = this
                    cancellable.resume(bodyInputStream())
                }
            }.run {
                getOrNull() ?: exceptionOrNull()?.let { e ->
                    cancellable.resumeWithException(
                        e.takeIf { it is RequestException }?.let {
                            response?.run {
                                RequestException(it, code, body?.byteStream()?.readBytes())
                            } ?: RequestException(it)
                        } ?: e
                    )
                } ?: throw IllegalStateException("No result and no exception")
            }
        }

    override fun cancel(api: ApiSpec) {
        callMap[api]?.run {
            cancel()
            callMap.remove(api)
        }
    }

    override fun cancelAll() {
        httpClient.dispatcher.cancelAll()
        callMap.clear()
    }

    @Throws(Throwable::class)
    private fun getResponse(api: ApiSpec): Response {
        return clientWith(api).newCall(generateRequest(api)).run {
            callMap[api] = this
            try {
                execute()
            } finally {
                callMap.remove(api)
            }
        }
    }

    @Throws(RequestException::class)
    private fun Response.bodyInputStream(): InputStream =
        if (isSuccessful) {
            body?.byteStream() ?: ByteArrayInputStream(ByteArray(0))
        } else {
            throw RequestException(
                IllegalStateException("Okhttp request unsuccessful.\n$message"),
                code,
                body?.byteStream()?.readBytes()
            )
        }

    private fun clientWith(api: ApiSpec) = api.timeout?.let {
        with(httpClient.newBuilder()) {
            readTimeout(it, TimeUnit.MILLISECONDS)
            build()
        }
    } ?: httpClient

    private fun generateRequest(api: ApiSpec): Request =
        with(Request.Builder()) {
            headers(headers(api))
            when (api.httpMethod) {
                HttpMethod.GET -> get()
                HttpMethod.POST -> post(generateBody(api))
                HttpMethod.PUT -> put(generateBody(api))
                HttpMethod.DELETE -> delete(generateBody(api))
                HttpMethod.PATCH -> patch(generateBody(api))
            }
            url(httpUrlWithQueries(api))
            build()
        }

    private fun httpUrlWithQueries(api: ApiSpec): HttpUrl {
        val urlBuilder = httpUrl(api).newBuilder()
        api.urlQueries.forEach { (key, value) ->
            urlBuilder.addQueryParameter(key, value)
        }
        api.urlArrayQueries.forEach {
            it.value.forEach { value ->
                urlBuilder.addQueryParameter(it.key, value)
            }
        }
        return urlBuilder.build()
    }

    private fun httpUrl(api: ApiSpec): HttpUrl {
        return api.url.toHttpUrl()
    }

    private fun headers(api: ApiSpec) = with(Headers.Builder()) {
        addAll(api.headers.toHeaders())
        api.contentType.takeIf { it.isNotBlank() }
            ?.let { add(CONTENT_TYPE, it) }
        build()
    }

    private fun generateBody(api: ApiSpec): RequestBody {
        return api.body.toRequestBody(api.contentType.toMediaType())
    }

}