package com.github.im.server.schema.migration.domain;

public record TenantTarget(
        Long companyId,
        String companyName,
        String schemaName,
        boolean active
) {
}
