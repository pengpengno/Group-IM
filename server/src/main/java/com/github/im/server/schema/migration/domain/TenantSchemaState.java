package com.github.im.server.schema.migration.domain;

import java.time.Instant;
import java.util.UUID;

public record TenantSchemaState(
        Long companyId,
        String schemaName,
        String currentVersion,
        String targetVersion,
        TenantSchemaStateStatus status,
        UUID lastRunId,
        String lastError,
        Instant updatedAt
) {
}
