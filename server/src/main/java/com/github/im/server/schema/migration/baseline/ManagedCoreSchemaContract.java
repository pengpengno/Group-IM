package com.github.im.server.schema.migration.baseline;

import org.flywaydb.core.api.MigrationVersion;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Version-aware expected semantics for core objects after a tenant has trusted
 * Flyway history.
 *
 * The immutable 2026081906 adoption contract is never changed. Managed core
 * evolution is expressed as a new contract selected only when Flyway history
 * proves the corresponding migration has been applied successfully.
 */
public record ManagedCoreSchemaContract(
        String contractVersion,
        Map<String, String> expectedCategoryHashes
) {

    public static final String WORKBENCH_STORAGE_VERSION = "2026082003";

    // Pinned from PostgreSQL Testcontainers after V2026082003. During initial
    // calibration the dedicated contract test will print the observed value.
    private static final String WORKBENCH_STORAGE_CONSTRAINTS_HASH =
            "UNSET_WORKBENCH_STORAGE_CONSTRAINTS_HASH";

    public ManagedCoreSchemaContract {
        expectedCategoryHashes = Map.copyOf(expectedCategoryHashes);
    }

    public static ManagedCoreSchemaContract adoptionBaseline() {
        return new ManagedCoreSchemaContract(
                CoreTenantBaselineContract.BASELINE_VERSION,
                CoreTenantBaselineContract.expectedCategoryHashes()
        );
    }

    public static ManagedCoreSchemaContract forManagedVersion(String currentVersion) {
        if (currentVersion == null || currentVersion.isBlank()) {
            throw new IllegalArgumentException("managed tenant current version is required");
        }

        MigrationVersion current = MigrationVersion.fromVersion(currentVersion);
        MigrationVersion workbenchStorage = MigrationVersion.fromVersion(WORKBENCH_STORAGE_VERSION);
        if (current.compareTo(workbenchStorage) < 0) {
            return adoptionBaseline();
        }

        Map<String, String> expected = new LinkedHashMap<>(
                CoreTenantBaselineContract.expectedCategoryHashes()
        );
        expected.put("constraints", WORKBENCH_STORAGE_CONSTRAINTS_HASH);
        return new ManagedCoreSchemaContract(WORKBENCH_STORAGE_VERSION, expected);
    }
}
