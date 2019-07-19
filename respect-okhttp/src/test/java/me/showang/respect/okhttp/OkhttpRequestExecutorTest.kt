package me.showang.respect.okhttp

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import me.showang.respect.RespectApi
import me.showang.respect.core.ContentType
import me.showang.respect.core.HttpMethod
import me.showang.respect.core.error.RequestError
import okhttp3.*
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import java.io.IOException
import java.util.concurrent.TimeUnit

class OkhttpRequestExecutorTest {

    @Test
    fun testApiCallback_success() {
        runBlocking {
            println("start testApiCallback_success on: ${Thread.currentThread().name}")
            MockGetApi().start(
                    OkhttpRequestExecutor(),
                    this,
                    { assert(false) { "may not be fail: $it" } },
                    ::checkResponse
            )
            delay(2000)
        }
    }

    @Test
    fun testApiSuspend_success() {
        runBlocking {
            println("start testApiSuspend_success on: ${Thread.currentThread().name}")
            val result = MockGetApi().suspend(OkhttpRequestExecutor())
            checkResponse(result)
        }
    }

    @Test
    fun testExecutor_suspend() {
        runBlocking {
            val result = OkhttpRequestExecutor().request(MockGetApi())
            checkResponse(String(result))
        }
    }

    private fun checkResponse(result: String) {
        println("checkResponse at thread: ${Thread.currentThread().name}")
        assert(result.isNotEmpty())
        assert(Thread.currentThread().name.startsWith("main"))
    }

    class MockGetApi : RespectApi<String>() {
        public override fun parse(bytes: ByteArray): String {
            println("parsing at thread: ${Thread.currentThread().name}")
            assert(Thread.currentThread().name.contains("worker"))
            return String(bytes)
        }

        override val url: String
            get() = "https://jsonplaceholder.typicode.com/comments"
        override val httpMethod: HttpMethod
            get() = HttpMethod.GET
        override val urlQueries: Map<String, String>
            get() = mapOf("postId" to "1")
    }

    class MockPostApi : RespectApi<String>() {
        override val httpMethod: HttpMethod
            get() = HttpMethod.POST
        override val url: String
            get() = "https://jsonplaceholder.typicode.com/posts"

        override val contentType: String
            get() = ContentType.JSON

        override val body: ByteArray
            get() {
                return "{\"title\": \"foo\", \"body\": \"bar\", \"userId\":1}".toByteArray()
            }

        override fun parse(bytes: ByteArray): String {
            return String(bytes).also { println("result: $it") }
        }
    }

    @Test
    fun testPOST_success() {
        runBlocking {
            try {
                val result = MockPostApi().suspend(OkhttpRequestExecutor())
            } catch (e: Throwable) {
                if (e is RequestError) {
                    println("Error: ${e.responseCode} ${String(e.bodyBytes ?: byteArrayOf())}")
                }
                assert(false)
            }

        }
    }

    class MockParseErrorApi : RespectApi<String>() {
        override val httpMethod: HttpMethod
            get() = HttpMethod.POST
        override val url: String
            get() = "https://jsonplaceholder.typicode.com/posts"

        override val contentType: String
            get() = ContentType.JSON

        override val body: ByteArray
            get() {
                return "{\"title\": \"foo\", \"body\": \"bar\", \"userId\":1}".toByteArray()
            }

        override fun parse(bytes: ByteArray): String {
            throw IllegalArgumentException("Parse Error")
        }
    }

    @Test
    fun testError() {
        runBlocking {
            MockParseErrorApi().start(OkhttpRequestExecutor(), this, {
                assert(true)
            }) {
                assert(false)
            }
        }
    }

    class IoExceptionApi : RespectApi<String>() {
        override val httpMethod: HttpMethod
            get() = HttpMethod.POST
        override val url: String
            get() = "https://jsonplaceholder.typicode.com/posts"

        override val contentType: String
            get() = ContentType.JSON

        override val body: ByteArray
            get() {
                return "{\"title\": \"foo\", \"body\": \"bar\", \"userId\":1}".toByteArray()
            }

        override fun parse(bytes: ByteArray): String {
            throw IOException("IOException")
        }
    }

    @Test
    fun testParsingException() {
        runBlocking {
            IoExceptionApi().start(OkhttpRequestExecutor(), this, {
                print("{testParsingException}\n $it")
            }) {
                assert(false)
            }
        }
    }

    @Test
    fun testRequestError() {
        val mockOkhttp = mock<OkHttpClient> {
            on { newBuilder() } doThrow IllegalStateException("Test Request Error")
        }
        val executor = OkhttpRequestExecutor(mockOkhttp)

        runBlocking {
            MockGetApi().start(executor, this, {
                print("{testRequestError}\n $it")
            }) {
                assert(false) {
                    "testRequestError must error."
                }
            }
        }
    }

    @Test
    fun testRequestErrorCode404() {
        val api = MockGetApi()
        val mockResponse = with(Response.Builder()){
            request(with(Request.Builder()) {
                headers(Headers.Builder().build())
                get()
                url("https://test_url")
                protocol(Protocol.HTTP_2)
                build()
            })
            code(404)
            body(ResponseBody.create(null, "mock 404 not found"))
            message("mock 404 not found")
            build()
        }
        val mockBuilder: OkHttpClient.Builder = mock()
        val mockCall: Call = mock {
            on { execute() } doReturn mockResponse
        }
        val mockOkhttp = mock<OkHttpClient> {
            on { newBuilder() } doReturn mockBuilder
            on { newCall(ArgumentMatchers.any(Request::class.java))} doReturn mockCall
        }
        `when`(mockBuilder.callTimeout(api.timeout, TimeUnit.MILLISECONDS)).thenReturn(mockBuilder)
        `when`(mockBuilder.build()).thenReturn(mockOkhttp)

        val requestExecutor = OkhttpRequestExecutor(mockOkhttp)

        runBlocking {
            api.start(requestExecutor, this, {
                assert(it is RequestError) {
                    "testRequestErrorCode404 must throw RequestError"
                }
                print(it)
            }) {
                assert(false) {
                    "testRequestErrorCode404 must throw RequestError"
                }
            }
        }

    }
}