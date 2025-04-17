package com.github.im.server.model;

import com.github.im.common.connect.model.proto.Chat;
import com.github.im.enums.MessageStatus;
import com.github.im.enums.MessageType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 消息实体类
@Entity
@Data
@NoArgsConstructor
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long msgId;  // 消息ID

//    private Long sessionId;  // 会话ID


//    private Long conversationId;  // 会话ID


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id",referencedColumnName = "conversationId" , nullable = false)
    private Conversation conversation;

    private String content;  // 消息内容

    private Long fromAccountId;  // 发送方ID

    private Long toAccountId;  // 接收方ID

    @Enumerated(EnumType.STRING)
    private MessageType type;  // 消息类型

    @Enumerated(EnumType.STRING)
    private MessageStatus status;  // 消息状态

    private LocalDateTime timestamp;  // 消息时间戳

    public Message(Long sessionId, String content, Long fromAccountId, Long toAccountId, MessageType type, MessageStatus status) {
//        this.sessionId = sessionId;
        this.content = content;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.type = type;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }



}
