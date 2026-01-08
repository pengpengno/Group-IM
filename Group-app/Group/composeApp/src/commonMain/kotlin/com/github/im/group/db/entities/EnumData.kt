package com.github.im.group.db.entities

import kotlinx.serialization.Serializable


/**
 * 消息状态
 */
@Serializable
enum class MessageStatus {
    SENDING,
    SENT,

    RECEIVED,
    FAILED,
    READ ,
    UNREAD ,
    DELETED,  // 删除
    REVOKE,  // 撤回
    ;


}
@Serializable

enum class ConversationStatus {
    ACTIVE,
    ACHEIVED,
    DELETED,
    INACTIVE
}
@Serializable

enum class FileStatus {
    UPLOADING, // 分块上传中
    NORMAL,
    DELETED,
    EXPIRED,
    FAILED,
    CHUNK_UPLOADING, // 分块上传中

    ;
}

/**
 * 消息的类型
 */
@Serializable
enum class MessageType {

    TEXT,
    FILE,
    VOICE,
    VIDEO,
    IMAGE,
    ;

    /**
     * 用于判断是否为文件类型 ， 音视频图像等都为文件
     */
    public fun isFile(): Boolean {
        return this == FILE || this == VOICE || this == VIDEO || this == IMAGE
    }
}


@Serializable
enum class UserStatus {
    ACTIVE,
    ONLINE,
    OFFLINE,
    AWAY,
    BUSY,
    INVISIBLE
}
