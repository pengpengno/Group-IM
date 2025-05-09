package com.github.im.group.gui.model;

import com.github.im.enums.MessageStatus;
import com.github.im.enums.MessageType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "message")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageTable {

    @Id
    /**
     * 服务端的id
     */
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long msgId;

    @Column(nullable = false)
    private Long seqId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MessageType messageType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MessageStatus status;

    @Column(nullable = false)
    private Long fromUserId;

    @Column(nullable = false)
    private Long conversationId;
}
