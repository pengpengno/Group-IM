package com.github.im.server.model;

import com.github.im.server.model.enums.Status;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "friendships")
@Data
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "friend_id", nullable = false)
    private User friend;

    @Enumerated(EnumType.STRING)
    private Status status;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;



    @PrePersist
    public void prePersist() {
        // 如果 status 为 null，则设置默认值
        if (status == null) {
            status = Status.ACTIVE;  // 例如，默认状态为 PENDING
        }
    }
}
