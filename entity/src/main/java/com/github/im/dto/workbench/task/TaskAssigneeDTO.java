package com.github.im.dto.workbench.task;

import java.time.LocalDateTime;

public record TaskAssigneeDTO(Long userId, String role, LocalDateTime createdAt) {
}
