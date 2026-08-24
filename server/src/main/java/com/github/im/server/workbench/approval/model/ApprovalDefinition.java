package com.github.im.server.workbench.approval.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name="wb_approval_definition") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ApprovalDefinition {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long definitionId;
    @Column(nullable=false, unique=true, length=64) private String code;
    @Column(nullable=false, length=120) private String name;
    @Column(nullable=false, columnDefinition="TEXT") private String formSchemaJson;
    @Column(nullable=false) private Boolean enabled;
    @Column(name="definition_version", nullable=false) private Integer definitionVersion;
    private Long createdBy;
    @CreationTimestamp @Column(nullable=false, updatable=false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(nullable=false) private LocalDateTime updatedAt;
}
