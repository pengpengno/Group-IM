package com.github.im.common.model.session.enums;

import com.github.im.common.connect.model.proto.Chat;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息文本类型
 * @author pengpeng
 * @description
 * @date 2023/1/16
 */
@Getter
@AllArgsConstructor
public enum ContentType {

    TEXT(Chat.MessageType.TEXT), // 文本

    FILE(Chat.MessageType.FILE) , // 文件

    MARKDOWN (Chat.MessageType.MARKDOWN)// markdown

    ;

    private final Chat.MessageType charMessageType;
}
