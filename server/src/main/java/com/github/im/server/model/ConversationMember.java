package com.github.im.server.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_members")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"conversation", "user"})
public class ConversationMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


//    @Column(nullable = false)
//    private Long userId;
//
//    @Column(nullable = false)
//    private Long conversationId;

    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "conversation_id",referencedColumnName = "conversation_id" , nullable = false)
    @JoinColumn(name = "conversation_id",referencedColumnName = "conversationId" , nullable = false)
    private Conversation conversation;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "userId",nullable = false)
//    @JoinColumn(name = "user_id", referencedColumnName = "user_id",nullable = false)
    private User user;


    @CreationTimestamp
    private LocalDateTime joinedAt;

    private LocalDateTime leftAt;

    /**
     * 该成员在此会话中已读到的最大 sequenceId（会话维度）。
     * 群聊已读/未读应基于成员维度推进，而不是全局修改 Message.status。
     */
    private Long lastReadSequenceId;

//
//    @PreUpdate
//    protected void onUpdate() {
//        this.joinedAt = LocalDateTime.now();
//    }

    @PrePersist
    protected void onPersist() {
        var now = LocalDateTime.now();
        this.joinedAt = now;
        if (this.lastReadSequenceId == null) {
            this.lastReadSequenceId = 0L;
        }
    }
}

