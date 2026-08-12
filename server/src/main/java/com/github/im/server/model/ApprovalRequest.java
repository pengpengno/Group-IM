package com.github.im.server.model;

import com.github.im.server.automation.AutomationApprovalStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Explicit approval record for a sensitive automation execution. */
@Entity
@Data
@Table(name = "approval_requests")
public class ApprovalRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long approvalId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id", nullable = false, unique = true)
    private AutomationExecution execution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AutomationApprovalStatus status;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime decidedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
