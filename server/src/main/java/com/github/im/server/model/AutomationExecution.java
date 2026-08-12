package com.github.im.server.model;

import com.github.im.server.automation.AutomationExecutionStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/** Durable execution intent.  It is intentionally created before an approval decision. */
@Entity
@Data
@Table(name = "automation_executions", uniqueConstraints = @UniqueConstraint(name = "uk_automation_execution_idempotency", columnNames = "idempotencyKey"))
public class AutomationExecution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long executionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id")
    private AutomationRule rule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @Column(nullable = false, length = 64)
    private String actionType;

    @Column(nullable = false, length = 512)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(length = 512)
    private String resultSummary;

    /** Message produced for a completed action, when its result belongs in a conversation. */
    private Long resultMessageId;

    @Column(nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AutomationExecutionStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
