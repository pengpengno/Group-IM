package com.github.im.dto.workbench.task;

import java.time.LocalDateTime;

public record CreateTaskRequest(
        String title,
        String description,
        String priority,
        Long ownerId,
        Long departmentId,
        Long conversationId,
        LocalDateTime startAt,
        LocalDateTime dueAt
) {
}
