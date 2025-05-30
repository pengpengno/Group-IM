package com.github.im.group.api

import ProxyApi
import io.ktor.http.HttpMethod
import kotlinx.serialization.Serializable

object LoginApi {
    suspend fun login(username: String, password: String): LoginResponse {
        val requestBody = LoginRequest(username, password)
        return ProxyApi.request(
            method = HttpMethod.Post,
            path = "/api/user/login",
            body = requestBody
        )
    }
}
@Serializable
data class LoginRequest(val username: String,
                        val password: String,
                        val refreshToken: String?="",
)

@Serializable
data class LoginResponse(val token: String, val userId: Long)
