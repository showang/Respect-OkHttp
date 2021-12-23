package me.showang.respect.okhttp

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import me.showang.respect.core.RequestExecutor
import me.showang.respect.core.error.ParseError
import me.showang.respect.core.error.RequestError
import me.showang.respect.okhttp.testapi.IoExceptionApi
import me.showang.respect.okhttp.testapi.MockGetApi
import me.showang.respect.okhttp.testapi.MockParseErrorApi
import me.showang.respect.okhttp.testapi.MockPostApi
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import java.io.IOException

class OkhttpRequestExecutorTest {

    private val executor: RequestExecutor = OkhttpRequestExecutor()

    @Test
    fun testApiSuspend_success() {
        runBlocking {
            println("start testApiSuspend_success on: ${Thread.currentThread().name}")
            val result = MockGetApi().request(executor)
            checkResponse(result)
        }
    }

    @Test
    fun testExecutor_suspend() {
        runBlocking {
            val result = OkhttpRequestExecutor().submit(MockGetApi())
            checkResponse(String(result.readBytes()))
        }
    }

    private fun checkResponse(result: String) {
        println("checkResponse at thread: ${Thread.currentThread().name}")
        assert(result.isNotEmpty())
    }

    @Test
    fun testPOST_success() {
        runBlocking {
            try {
                MockPostApi().request(OkhttpRequestExecutor())
            } catch (e: Throwable) {
                if (e is RequestError) {
                    println("Error: ${e.responseCode} ${String(e.bodyBytes ?: byteArrayOf())}")
                }
                assert(false)
            }

        }
    }

    @Test
    fun testError() {
        runBlocking {
            try {
                MockParseErrorApi().request(executor)
                assert(false) { "Should not be success" }
            } catch (e: Throwable) {
                assert(e is ParseError)
                assert(e.cause is IllegalArgumentException)
            }
        }
    }

    @Test
    fun testParsingException() {
        runBlocking {
            try {
                IoExceptionApi().request(executor)
                assert(false) { "Should not be success" }
            } catch (e: Throwable) {
                assert(e is ParseError)
                assert(e.cause is IOException)
                print("{testParsingException}\n $e")
            }
        }
    }

    @Test
    fun testRequestErrorCode404() {
        val mockResponse = with(Response.Builder()) {
            request(with(Request.Builder()) {
                headers(Headers.Builder().build())
                get()
                url("https://test_url")
                protocol(Protocol.HTTP_2)
                build()
            })
            code(404)
            body("mock 404 not found".toResponseBody())
            message("mock 404 not found")
            build()
        }
        val mockCall: Call = mockk(relaxed = true)
        val mockOkhttp: OkHttpClient = mockk(relaxed = true)
        every { mockOkhttp.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns mockResponse

        runBlocking {
            try {
                MockGetApi().request(OkhttpRequestExecutor(mockOkhttp))
                assert(false) {
                    "testRequestErrorCode404 must throw RequestError"
                }
            } catch (e: Throwable) {
                when (e) {
                    is RequestError -> assert(e.responseCode == 404) { "response code should be 404" }
                    else -> assert(false) { "testRequestErrorCode 404 must throw RequestError" }
                }
            }
        }

    }
}