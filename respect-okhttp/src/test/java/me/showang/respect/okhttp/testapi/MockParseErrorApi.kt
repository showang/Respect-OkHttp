package me.showang.respect.okhttp.testapi

import me.showang.respect.RestfulApi
import me.showang.respect.core.ContentType
import me.showang.respect.core.HttpMethod

class MockParseErrorApi : RestfulApi<String>() {
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