package com.github.im.server.schema.migration.baseline;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Immutable contract used to decide whether an existing tenant can be explicitly
 * baselined into Flyway history.
 *
 * 2026081906 remains the business-schema adoption baseline forever. The managed
 * target advances only through immutable Flyway migrations. Managed core evolution
 * after the baseline is validated separately and must never rewrite these pinned
 * adoption hashes.
 */
public final class CoreTenantBaselineContract {

    public static final String BASELINE_VERSION = "2026081906";
    public static final String MANAGED_TARGET_VERSION = "2026082004";

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

    private static final Map<String, String> EXPECTED_CATEGORY_HASHES = Map.of(
            "tables", "122b3038e1bd499e13d17dc4dcece6727ee3258baa0efacc07c90daba9176f6c",
            "columns", "8b2e23ea32c10b80679d4c2a91393655b0ee67c723432d6c60604b971f3670e0",
            "constraints", "16ff0b120c24d05f5390f6894eb5e36a0caf04cdc2077dc6beaa4fb11b3db813",
            "indexes", "2a3448b065e1ada6aad09a50cb4a6620aa5e1ded007e01cc519123696301f107",
            "views", "e095c9441ac9ee358d8d48811fe5ec878ee9052337f7f251d1deff6d721294c3",
            "sequences", "9c7b23697b6c965badf2b167eceac7e3a21aa10bb46ea636fdc2bbc5a3790150"
    );

    private CoreTenantBaselineContract() {
    }

    public static Map<String, String> expectedCategoryHashes() {
        return new LinkedHashMap<>(EXPECTED_CATEGORY_HASHES);
    }
}
