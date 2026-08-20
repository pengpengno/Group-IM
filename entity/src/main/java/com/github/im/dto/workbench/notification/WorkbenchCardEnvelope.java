package com.github.im.dto.workbench.notification;

import java.time.Instant;

public record WorkbenchCardEnvelope(
        int version,
        String eventId,
        WorkbenchCategory category,
        String action,
        String resourceId,
        Long companyId,
        String title,
        String summary,
        String fallbackText,
        String status,
        Instant occurredAt,
        String deepLink
) {
}
