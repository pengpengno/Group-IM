package com.github.im.group.db.entities

/**
 * 好友请求状态枚举
 */
enum class FriendRequestStatus {
    /**
     * 等待接受
     */
    PENDING,

    /**
     * 已接受
     */
    ACCEPTED,

    /**
     * 已拒绝
     */
    REJECTED,

    /**
     * 单向删除
     */
    DISCORD,


    /**
     * 已忽略
     */
    IGNORED,

    /**
     * 已屏蔽
     */
    BLOCKED
}