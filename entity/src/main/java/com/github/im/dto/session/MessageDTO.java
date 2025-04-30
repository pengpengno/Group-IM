package com.github.im.dto.session;

import com.github.im.dto.user.UserInfo;
import com.github.im.enums.MessageStatus;
import com.github.im.enums.MessageType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageDTO<T extends MessagePayLoad> {

    private Long msgId;
    private Long conversationId;  // 会话ID
    private String content;
    private Long fromAccountId;
    private Long sequenceId;

    private UserInfo fromAccount;

    private MessageType type;
    private MessageStatus status;
    private LocalDateTime timestamp;

    // 消息拓展体
    /**
     *   当消息类型 不为文本{@link MessageType#TEXT }，正常文件 就传入原值即可
     *   且需要一些 额外的拓展信息时，传入此字段
     */
    private T payload;

}
