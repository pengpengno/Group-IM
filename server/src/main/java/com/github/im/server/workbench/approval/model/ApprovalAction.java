package com.github.im.server.workbench.approval.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name="wb_approval_action") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ApprovalAction {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long actionId;
    @Column(nullable=false) private Long instanceId;
    private Long nodeId;
    @Column(nullable=false) private Long operatorId;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=24) private ApprovalActionType action;
    @Column(columnDefinition="TEXT") private String comment;
    @CreationTimestamp @Column(nullable=false, updatable=false) private LocalDateTime createdAt;
}
