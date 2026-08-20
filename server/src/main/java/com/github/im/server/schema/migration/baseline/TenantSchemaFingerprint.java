package com.github.im.server.schema.migration.baseline;

import java.util.Map;
import java.util.Set;

public record TenantSchemaFingerprint(
        String fingerprint,
        Map<String, String> categoryHashes,
        Set<String> tables,
        Set<String> views,
        boolean identityViewsValid
) {
}
