package com.github.im.server.model;

import com.github.im.server.model.enums.Status;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    private String remark; // 备注

    private String applyRemark; // 申请关系时的备注

    @CreationTimestamp // 自动设置创建时间
    @Column(updatable = false) // 创建时间不可被更新
    private LocalDateTime createdAt = LocalDateTime.now();

    @UpdateTimestamp // 自动设置更新时间
    private LocalDateTime updatedAt;



    @PrePersist
    public void prePersist() {
        // 如果 status 为 null，则设置默认值
        if (status == null) {
            status = Status.PENDING;  // 例如，默认状态为 PENDING
        }
    }



}
