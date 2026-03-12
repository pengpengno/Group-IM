package com.github.im.dto.message;

import com.github.im.enums.MessageType;
import lombok.Data;

@Data
public class MessagePostRequest {
    private Long conversationId;
    private String content;
    private MessageType type;
    private String clientMsgId;
}
