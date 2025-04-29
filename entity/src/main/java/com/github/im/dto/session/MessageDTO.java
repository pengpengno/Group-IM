package com.github.im.dto.session;

import com.github.im.dto.user.UserInfo;
import com.github.im.enums.MessageStatus;
import com.github.im.enums.MessageType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageDTO {

    private Long msgId;
    private Long conversationId;  // 会话ID
    private String content;
    private Long fromAccountId;
    private Long sequenceId;

    private UserInfo fromAccount;

    private MessageType type;
    private MessageStatus status;
    private LocalDateTime timestamp;

}
