package me.showang.respect.okhttp.github.api

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import me.showang.respect.RestfulApi
import me.showang.respect.core.HttpMethod

class UserDetailApi(account: String) : RestfulApi<UserDetailApi.UserDetailServerEntity>() {
    override val httpMethod = HttpMethod.GET
    override val url = "https://api.github.com/users/$account"

    override val headers: Map<String, String>
        get() = mapOf(
            "Accept" to "application/vnd.github.v3+json"
        )

    override fun parse(bytes: ByteArray): UserDetailServerEntity {
        return Gson().fromJson(String(bytes), UserDetailServerEntity::class.java)
    }

    data class UserDetailServerEntity(
        @SerializedName("id") val id: Long,
        @SerializedName("login") val login: String,
        @SerializedName("avatar_url") val avatarUrl: String,
        @SerializedName("name") val name: String?,
        @SerializedName("bio") val bio: String?,
        @SerializedName("site_admin") val siteAdmin: Boolean,
        @SerializedName("location") val location: String?,
        @SerializedName("blog") val blog: String?,
        @SerializedName("followers") val followers: Int?,
        @SerializedName("following") val following: Int?,
        @SerializedName("email") val email: String?
    )
}