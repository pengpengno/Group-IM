package com.github.im.server.workbench.approval.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name="wb_approval_instance") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ApprovalInstance {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long instanceId;
    @Column(nullable=false) private Long definitionId;
    @Column(nullable=false) private Integer definitionVersion;
    @Column(nullable=false, length=200) private String title;
    @Column(nullable=false) private Long applicantId;
    private Long departmentId;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=24) private ApprovalStatus status;
    @Column(nullable=false, columnDefinition="TEXT") private String formDataJson;
    private Integer currentNodeOrder;
    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;
    @Version private Long version;
    @CreationTimestamp @Column(nullable=false, updatable=false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(nullable=false) private LocalDateTime updatedAt;
}
