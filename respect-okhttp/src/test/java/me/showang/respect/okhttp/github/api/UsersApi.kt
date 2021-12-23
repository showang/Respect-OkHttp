package me.showang.respect.okhttp.github.api

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import me.showang.respect.RestfulApi
import me.showang.respect.core.HttpMethod
import java.lang.reflect.Type

class UsersApi(sinceId: Long? = null, private val pageSize: Int = 20) :
    RestfulApi<UsersApi.Result>() {
    override val httpMethod = HttpMethod.GET
    override val url = "https://api.github.com/users"
    override val urlQueries: Map<String, String> = mutableMapOf<String, String>().apply {
        sinceId?.let { put("since", it.toString()) }
        put("per_page", pageSize.toString())
    }
    override val headers: Map<String, String>
        get() = mapOf(
            "Accept" to "application/vnd.github.v3+json"
        )

    override fun parse(bytes: ByteArray): Result {
        val type: Type = object : TypeToken<List<UserInfoServerEntity>>() {}.type
        val infoList = Gson().fromJson<List<UserInfoServerEntity>>(String(bytes), type)
        return Result(infoList, infoList.size == pageSize)
    }

    data class Result(
        val infoList: List<UserInfoServerEntity>,
        val hasNextPage: Boolean
    )

    data class UserInfoServerEntity(
        @SerializedName("id") val id: Long,
        @SerializedName("login") val login: String,
        @SerializedName("avatar_url") val avatarUrl: String,
        @SerializedName("site_admin") val siteAdmin: Boolean
    )
}