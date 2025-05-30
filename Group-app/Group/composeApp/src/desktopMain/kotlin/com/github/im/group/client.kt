package com.github.im.group

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.engine.cio.* // JVM
// or .darwin / .js for other platforms

val client = HttpClient(CIO)
suspend fun login(username: String, password: String): String {
    return client.post("https://your.api/login") {
        parameter("username", username)
        parameter("password", password)
    }
}
