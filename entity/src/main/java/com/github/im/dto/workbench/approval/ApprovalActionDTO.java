package com.github.im.dto.workbench.approval;
import java.time.LocalDateTime;
public record ApprovalActionDTO(Long actionId, Long nodeId, Long operatorId, String action,
                                String comment, LocalDateTime createdAt) { }
