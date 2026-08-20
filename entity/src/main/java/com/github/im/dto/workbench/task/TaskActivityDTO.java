package com.github.im.dto.workbench.task;

import java.time.LocalDateTime;

public record TaskActivityDTO(
        Long activityId,
        Long actorId,
        String action,
        String beforeState,
        String afterState,
        String detail,
        LocalDateTime createdAt
) {
}
