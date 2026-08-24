package com.github.im.server.workbench.approval.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="wb_approval_node") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ApprovalNode {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long nodeId;
    @Column(nullable=false) private Long instanceId;
    @Column(nullable=false) private Integer nodeOrder;
    @Column(nullable=false) private Long assigneeId;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=24) private ApprovalNodeStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    @Version private Long version;
}
