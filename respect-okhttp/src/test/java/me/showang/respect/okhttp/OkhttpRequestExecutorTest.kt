package me.showang.respect.okhttp

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import me.showang.respect.RespectApi
import me.showang.respect.core.HttpMethod
import org.junit.Test

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
}