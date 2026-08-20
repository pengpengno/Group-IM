package com.github.im.dto.workbench.notification;

import java.time.Instant;

public record WorkbenchEventEnvelope(
        int version,
        String eventId,
        WorkbenchCategory category,
        String action,
        String resourceId,
        Long companyId,
        Instant occurredAt,
        String deepLink
) {
}
