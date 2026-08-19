package com.github.im.server.schema.migration.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MigrationRunSnapshot(
        UUID runId,
        MigrationMode mode,
        MigrationRunStatus status,
        Long requestedBy,
        Instant requestedAt,
        Instant startedAt,
        Instant completedAt,
        int totalCount,
        int successCount,
        int failedCount,
        List<Item> items
) {
    public record Item(
            long itemId,
            Long companyId,
            String schemaName,
            MigrationItemStatus status,
            String fromVersion,
            String targetVersion,
            int pendingCount,
            Instant startedAt,
            Instant completedAt,
            Long durationMs,
            String errorMessage
    ) {
    }
}
