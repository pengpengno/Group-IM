package com.github.im.server.schema.migration.baseline;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Immutable contract used to decide whether an existing tenant can be explicitly
 * baselined into Flyway history.
 *
 * 2026081906 is the business-schema baseline. 2026082001 is the first
 * non-destructive migration that must run after an explicit baseline.
 */
public final class CoreTenantBaselineContract {

    public static final String BASELINE_VERSION = "2026081906";
    public static final String MANAGED_TARGET_VERSION = "2026082001";

    public static final Set<String> CORE_TABLES = Set.of(
            "approval_requests",
            "automation_executions",
            "automation_rules",
            "conversation_bot_configs",
            "conversation_members",
            "conversations",
            "departments",
            "file_resource",
            "friendships",
            "media_file_resource",
            "meeting_participants",
            "meetings",
            "messages",
            "status_updates",
            "system_config_item",
            "upload_chunk_record",
            "user_departments",
            "user_privacy_settings"
    );

    public static final Set<String> IDENTITY_VIEWS = Set.of(
            "company",
            "company_user",
            "users"
    );

    /**
     * Category hashes are generated from a PostgreSQL 16 schema created only by
     * the immutable tenant migrations. They intentionally exclude Flyway and
     * tenant_schema_metadata tables. The fingerprint queries use semantic catalog
     * fields so the same contract can be compared on PostgreSQL 14+.
     *
     * Values are pinned by CoreTenantBaselineContractIntegrationTest.
     */
    private static final Map<String, String> EXPECTED_CATEGORY_HASHES = Map.of(
            "tables", "UNSET_TABLES",
            "columns", "UNSET_COLUMNS",
            "constraints", "UNSET_CONSTRAINTS",
            "indexes", "UNSET_INDEXES",
            "views", "UNSET_VIEWS",
            "sequences", "UNSET_SEQUENCES"
    );

    private CoreTenantBaselineContract() {
    }

    public static Map<String, String> expectedCategoryHashes() {
        return new LinkedHashMap<>(EXPECTED_CATEGORY_HASHES);
    }
}
