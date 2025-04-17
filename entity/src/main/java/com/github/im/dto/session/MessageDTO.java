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
    private MessageType type;
    private MessageStatus status;
    private LocalDateTime timestamp;

}
