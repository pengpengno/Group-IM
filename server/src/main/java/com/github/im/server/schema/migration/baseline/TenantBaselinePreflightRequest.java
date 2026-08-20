package com.github.im.server.schema.migration.baseline;

import java.util.List;

public record TenantBaselinePreflightRequest(
        List<Long> companyIds,
        boolean allActive
) {
}
