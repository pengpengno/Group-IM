package com.github.im.group.api

import io.ktor.http.HttpMethod
import kotlinx.serialization.Serializable

@Serializable
data class WorkbenchApiEnvelope<T>(
    val code: Int,
    val message: String = "",
    val data: T? = null,
)

@Serializable
data class WorkbenchTaskDetail(
    val taskId: Long,
    val title: String,
    val description: String? = null,
    val status: String,
    val priority: String,
    val creatorId: Long,
    val ownerId: Long? = null,
    val departmentId: Long? = null,
    val conversationId: Long? = null,
    val startAt: String? = null,
    val dueAt: String? = null,
    val completedAt: String? = null,
    val progress: Int = 0,
    val version: Long = 0,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

object WorkbenchApi {
    suspend fun taskDetail(taskId: Long): WorkbenchTaskDetail {
        val response = ProxyApi.request<Unit, WorkbenchApiEnvelope<WorkbenchTaskDetail>>(
            hmethod = HttpMethod.Get,
            path = "/api/workbench/tasks/$taskId",
        )
        if (response.code !in 200..299) {
            throw ClientRequestException(response.message.ifBlank { "工作台任务加载失败" })
        }
        return response.data ?: throw ClientRequestException("工作台任务不存在或无权访问")
    }
}
