package com.github.im.server.schema.migration.baseline;

import java.util.Map;
import java.util.Set;

/**
 * Core baseline fingerprint plus the complete tenant object inventory needed for
 * safe adoption decisions.
 *
 * tables/views/sequences are scoped to the immutable 2026081906 core baseline
 * and therefore participate in the pinned fingerprint. all* sets are inventory
 * only and never change the pinned core hashes.
 */
public record TenantSchemaFingerprint(
        String fingerprint,
        Map<String, String> categoryHashes,
        Set<String> tables,
        Set<String> views,
        Set<String> sequences,
        Set<String> allTables,
        Set<String> allViews,
        Set<String> allSequences,
        boolean identityViewsValid
) {
}
