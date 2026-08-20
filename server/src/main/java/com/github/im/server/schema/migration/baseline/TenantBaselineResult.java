package com.github.im.server.schema.migration.baseline;

import java.util.UUID;

public record TenantBaselineResult(
        UUID auditId,
        Long companyId,
        String schemaName,
        String baselineVersion,
        String currentVersion,
        String fingerprint
) {
}
