package com.github.im.dto.workbench.overview;

import java.time.LocalDateTime;

public record WorkbenchApprovalSummaryDTO(Long instanceId, String title, String status, LocalDateTime submittedAt) {
}
