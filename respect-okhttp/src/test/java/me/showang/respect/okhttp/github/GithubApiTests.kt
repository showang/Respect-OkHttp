package me.showang.respect.okhttp.github

import kotlinx.coroutines.runBlocking
import me.showang.respect.okhttp.OkhttpRequestExecutor
import me.showang.respect.okhttp.github.api.UserDetailApi
import me.showang.respect.okhttp.github.api.UsersApi
import org.junit.Test

class GithubApiTests {

    private val executor = OkhttpRequestExecutor()

    @Test
    fun testUserDetailApi() {
        runBlocking {
            UserDetailApi("showang").request(executor)
        }.let { result ->
            assert(result.login == "showang")
            assert(result.name == "Hsuanhao")
            assert(result.bio == "Philosopher")
            assert(result.location == "Taiwan")
        }
    }

    @Test
    fun testUsersApi() {
        runBlocking {
            UsersApi().request(executor)
        }.let { result ->
            assert(result.infoList.size == 20)
            assert(result.hasNextPage)
        }
    }

}