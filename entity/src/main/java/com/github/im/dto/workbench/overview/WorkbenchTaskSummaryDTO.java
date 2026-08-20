package com.github.im.dto.workbench.overview;

import java.time.LocalDateTime;

public record WorkbenchTaskSummaryDTO(Long taskId, String title, String status, LocalDateTime dueAt) {
}
