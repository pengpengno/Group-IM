package com.github.im.dto.workbench.task;

import java.time.LocalDateTime;

public record TaskSummaryDTO(
        Long taskId,
        String title,
        String status,
        String priority,
        Long ownerId,
        LocalDateTime dueAt,
        int progress,
        LocalDateTime updatedAt
) {
}
