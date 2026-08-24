package com.github.im.dto.workbench.approval;
import java.time.LocalDateTime;
public record ApprovalSummaryDTO(Long instanceId, String title, Long applicantId, String status,
                                 Integer currentNodeOrder, LocalDateTime submittedAt, LocalDateTime updatedAt) { }
