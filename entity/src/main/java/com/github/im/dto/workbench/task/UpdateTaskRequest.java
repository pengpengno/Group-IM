package com.github.im.dto.workbench.task;

import java.time.LocalDateTime;

public record UpdateTaskRequest(
        String title,
        String description,
        String priority,
        LocalDateTime dueAt,
        Integer progress
) {
}
