package com.github.im.dto.session;

import com.github.im.enums.MessageStatus;
import com.github.im.enums.MessageType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageDTO {

    private Long msgId;
    private Long sessionId;
    private String content;
    private Long fromAccountId;
    private Long toAccountId;
    private MessageType type;
    private MessageStatus status;
    private LocalDateTime timestamp;

    public MessageDTO(Long msgId, Long sessionId, String content, Long fromAccountId, Long toAccountId, MessageType type, MessageStatus status, LocalDateTime timestamp) {
        this.msgId = msgId;
        this.sessionId = sessionId;
        this.content = content;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.type = type;
        this.status = status;
        this.timestamp = timestamp;
    }
}
