package com.github.im.server.workbench.approval.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name="wb_approval_cc") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ApprovalCc {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long ccId;
    @Column(nullable=false) private Long instanceId;
    @Column(nullable=false) private Long userId;
    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;
    @CreationTimestamp @Column(nullable=false, updatable=false) private LocalDateTime createdAt;
}
