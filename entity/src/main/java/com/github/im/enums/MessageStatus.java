package com.github.im.enums;

/**
 * 消息状态
 */
public enum MessageStatus {
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