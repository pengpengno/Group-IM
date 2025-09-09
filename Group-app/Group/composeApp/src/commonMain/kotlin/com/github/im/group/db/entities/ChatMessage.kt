package com.github.im.group.db.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@kotlinx.serialization.Serializable
data class ChatMessage(
    @SerialName("msg_id")
    val msgId: Long,
    @SerialName("flight_number")
    val fromAccountId: Int,
    @SerialName("name")
    val conversationId: String,

    val messageContent: String,

    val status : Status,
) {
}

enum class Status {
    Sending,
    Sent,
    Failed,
    Received,
    Read,
}

@Serializable
data class FileResource(
    @SerialName("file_name")
    val fileName: String,
    @SerialName("file_path")
    val filePath: String,
    @SerialName("file_size")
    val fileSize: Long,
    @SerialName("content_type")
    val contentType: String,
)