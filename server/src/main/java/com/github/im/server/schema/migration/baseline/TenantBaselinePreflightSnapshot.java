package com.github.im.server.schema.migration.baseline;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record TenantBaselinePreflightSnapshot(
        Long companyId,
        String companyName,
        String schemaName,
        TenantBaselineClassification classification,
        String baselineVersion,
        boolean historyPresent,
        String observedFingerprint,
        Map<String, String> categoryHashes,
        List<String> repairPlan,
        Instant checkedAt
) {
}
