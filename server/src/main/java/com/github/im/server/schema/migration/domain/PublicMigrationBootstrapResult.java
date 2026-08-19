package com.github.im.server.schema.migration.domain;

public record PublicMigrationBootstrapResult(
        String currentVersion,
        boolean baselineCreated
) {
}
