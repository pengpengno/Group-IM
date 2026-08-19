package com.github.im.server.schema.migration.api;

import java.util.UUID;

public record MigrationRunAccepted(
        UUID runId,
        String status
) {
}
