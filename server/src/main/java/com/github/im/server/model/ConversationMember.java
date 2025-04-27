package com.github.im.server.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_members")
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



    private LocalDateTime joinedAt;

    private LocalDateTime leftAt;

//
//    @PreUpdate
//    protected void onUpdate() {
//        this.joinedAt = LocalDateTime.now();
//    }

    @PrePersist
    protected void onPersist() {
        var now = LocalDateTime.now();
        this.joinedAt = now;
    }
}

