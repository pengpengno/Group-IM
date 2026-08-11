package com.github.im.group.update

import com.github.im.group.api.ProxyApi
import com.github.im.group.model.ApiResponse
import io.ktor.http.HttpMethod

object AppUpdateApi {
    const val ANDROID_STABLE_PATH = "/api/app-updates/android"
    suspend fun checkAndroid(packageName: String, versionCode: Int): AppUpdateManifest? =
        ProxyApi.request<Unit, ApiResponse<AppUpdateManifest>>(
            hmethod = HttpMethod.Get, path = ANDROID_STABLE_PATH,
            requestParams = mapOf("packageName" to packageName, "versionCode" to versionCode.toString(), "channel" to "stable")
        ).data
}
