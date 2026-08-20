package com.github.im.dto.workbench.overview;

import java.time.LocalDateTime;

public record WorkbenchScheduleSummaryDTO(
        String type,
        Long resourceId,
        String title,
        String status,
        LocalDateTime startsAt,
        LocalDateTime endsAt
) {
}
