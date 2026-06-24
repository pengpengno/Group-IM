package com.github.im.server.model;

import com.github.im.enums.MessageStatus;
import com.github.im.enums.MessageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long msgId;  // 消息ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", referencedColumnName = "conversationId", nullable = false)
    private Conversation conversation;  // 所属会话

    /** 客户端生成的消息唯一ID，用于去重和回执对齐。 */
    private String clientMsgId;

    /**
     * 消息内容。
     *
     * <p>不同类型的消息会复用这个字段：</p>
     * <ul>
     *     <li>文本消息：直接存纯文本</li>
     *     <li>文件 / 图片 / 视频消息：通常存文件资源ID</li>
     *     <li>会议 / 系统消息：可能存序列化后的 JSON 结构</li>
     * </ul>
     *
     * <p>会议总结这类富结构消息序列化后很容易超过 255 个字符，
     * 所以这里必须明确映射为 TEXT，避免线上再次出现长度溢出。</p>
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;  // 消息正文

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id", referencedColumnName = "userId", nullable = false)
    private User fromAccountId;  // 发送人

    /**
     * 服务端分配的会话内序列号。
     *
     * <p>该值只在单个会话内递增，用于消息排序、补拉和已读推进。</p>
     */
    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long sequenceId;

    @Enumerated(EnumType.STRING)
    private MessageType type;  // 消息类型

    @Enumerated(EnumType.STRING)
    private MessageStatus status;  // 消息状态

    private LocalDateTime clientTimestamp;  // 客户端发送时间

    private LocalDateTime timestamp;  // 服务端消息时间

    private LocalDateTime createTime;  // 数据库创建时间

    @PrePersist
    protected void onPersist() {
        // 持久化前兜底写入创建时间，避免上层遗漏赋值。
        createTime = LocalDateTime.now();
    }
}
