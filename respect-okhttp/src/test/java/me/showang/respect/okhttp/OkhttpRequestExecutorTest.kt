package me.showang.respect.okhttp

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import me.showang.respect.RespectApi
import me.showang.respect.core.ApiSpec
import me.showang.respect.core.ContentType
import me.showang.respect.core.HttpMethod
import me.showang.respect.core.RequestError
import org.junit.Test
import java.io.IOException

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
    fun testIOException() {
        runBlocking {
            IoExceptionApi().start(object: OkhttpRequestExecutor() {
                override suspend fun request(api: ApiSpec): ByteArray {
                    throw IOException("test")
                }
            }, this, {
                assert(true)
            }) {
                assert(false)
            }
        }
    }
}