package me.showang.respect.okhttp.testapi

import me.showang.respect.RestfulApi
import me.showang.respect.core.HttpMethod

class MockQueryArrayApi : RestfulApi<String>() {
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
    override val urlArrayQueries: Map<String, List<String>>
        get() = mapOf(
            "testArray[]" to (0..10).map { it.toString() }
        )
}