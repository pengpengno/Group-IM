package com.github.im.dto.workbench.approval;
import java.time.LocalDateTime;
import java.util.List;
public record ApprovalInstanceDTO(Long instanceId, Long definitionId, int definitionVersion,
        String title, Long applicantId, Long departmentId, String status, String formDataJson,
        Integer currentNodeOrder, long version, LocalDateTime submittedAt, LocalDateTime completedAt,
        LocalDateTime createdAt, LocalDateTime updatedAt, List<ApprovalNodeDTO> nodes,
        List<ApprovalActionDTO> actions, List<Long> ccUserIds, List<String> availableActions) { }
