package me.showang.respect.okhttp

import me.showang.respect.HttpMethod
import me.showang.respect.RequestExecutor
import me.showang.respect.base.SimpleApi
import org.junit.Before
import org.junit.Test

class OkhttpRequestExecutorTest {

    private lateinit var executor: RequestExecutor

    @Before
    fun setup() {
        executor = OkhttpRequestExecutor(syncMode = true)
    }


    @Test
    fun testRequest_success() {
        MockGetApi().start(executor, successHandler = ::print)
    }

    private fun print(s: String) = System.out.println(s)

    class MockGetApi : SimpleApi<String, MockGetApi>() {
        override fun parse(bytes: ByteArray): String {
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