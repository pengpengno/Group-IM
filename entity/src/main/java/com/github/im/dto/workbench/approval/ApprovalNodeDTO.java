package com.github.im.dto.workbench.approval;
import java.time.LocalDateTime;
public record ApprovalNodeDTO(Long nodeId, int nodeOrder, Long assigneeId, String status,
                              LocalDateTime startedAt, LocalDateTime completedAt) { }
