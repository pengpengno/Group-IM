package com.github.im.group.db.entities


/**
 * 消息状态
 */
enum class MessageStatus {
    SENDING,
    SENT,
    FAILED,
    RECEIVED,
    READ,
}
enum class ConversationStatus {
    ACTIVE,
    ACHEIVED,
    DELETED,
    INACTIVE
}
enum class FileStatus {
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
enum class MessageType {

    TEXT,
    FILE,
    VOICE,
    VIDEO,
    IMAGE,
    ;
}

enum class UserStatus {

    ONLINE,
    OFFLINE,
    AWAY,
    BUSY,
    INVISIBLE
}


