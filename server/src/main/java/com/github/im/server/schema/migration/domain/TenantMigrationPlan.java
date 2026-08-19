package com.github.im.server.schema.migration.domain;

import java.util.List;

public record TenantMigrationPlan(
        String schemaName,
        String currentVersion,
        String targetVersion,
        int pendingCount,
        List<String> pendingMigrations,
        boolean blocked,
        String blockedReason
) {
}
