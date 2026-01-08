package com.github.im.server.model;

import com.github.im.enums.MessageStatus;
import com.github.im.enums.MessageType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// 消息实体类
@Entity
@Data
@NoArgsConstructor
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long msgId;  // 消息ID


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id",referencedColumnName = "conversationId" , nullable = false)
    private Conversation conversation;

    /**UUID**/
    private String clientMsgId;

    /**
     * 消息内容
     * TODO 需要设计下实现类 将不同类型的消息先根据 消息类型判断  ； 然后根据设计好的 decode 和 encode 方法进行序列化和反序列化
     *
     * <ul>
     *     <li>文本消息</li>
     *     <li>文件 会使用 {@link FileResource#getId()}</li>
     *     <li>链接消息</li>
     * </ul>
     */
    private String content;  // 消息内容

//    private Long fromAccountId;  // 发送方ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id",referencedColumnName = "userId" , nullable = false)
    private User fromAccountId;


    /**
     * 服务端分配的 会话中消息的序列号 ；
     * 会话中的sequenceId 是递增的， 不重复的； 且会话之间相互独立
     */
    @Column( nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long sequenceId ;  // 服务端 分配的会话序列号

    @Enumerated(EnumType.STRING)
    private MessageType type;  // 消息类型

    @Enumerated(EnumType.STRING)
    private MessageStatus status;  // 消息状态



    private LocalDateTime clientTimestamp;  // 客户端发送时间

    private LocalDateTime timestamp;  // 消息时间戳

    private LocalDateTime createTime;  // 消息创建时间


    @PrePersist
    protected void onPersist() {
        createTime = LocalDateTime.now();;
    }

}