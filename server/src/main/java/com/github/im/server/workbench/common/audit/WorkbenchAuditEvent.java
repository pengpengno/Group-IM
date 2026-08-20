package com.github.im.server.workbench.common.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record WorkbenchAuditEvent(
        UUID eventId,
        Long companyId,
        String schemaName,
        Long actorUserId,
        String category,
        String action,
        String resourceType,
        String resourceId,
        String beforeState,
        String afterState,
        Instant occurredAt,
        Map<String, Object> metadata
) {
    public WorkbenchAuditEvent {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
