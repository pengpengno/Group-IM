package com.github.im.server.schema.migration.baseline;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Versioned semantic contract for core objects that legitimately evolve after
 * the immutable 2026081906 adoption baseline.
 *
 * The baseline itself is never changed. A managed tenant may project a known,
 * Flyway-proven evolution back to its baseline representation only when the
 * current object exactly matches a known managed contract.
 */
public final class ManagedCoreSchemaContract {

    public static final String WORKBENCH_STORAGE_VERSION = "2026082003";

    public static final Set<String> BASELINE_MESSAGE_TYPES = Set.of(
            "TEXT", "FILE", "VOICE", "VIDEO", "IMAGE", "MEDIA", "MEETING", "BOT_CARD"
    );

    public static final Set<String> WORKBENCH_MESSAGE_TYPES = Set.of(
            "TEXT", "FILE", "VOICE", "VIDEO", "IMAGE", "MEDIA", "MEETING", "BOT_CARD", "WORKBENCH"
    );

    private static final String MESSAGE_TYPE_ROW_PREFIX = "messages|messages_type_check|c|";
    private static final String WORKBENCH_NORMALIZED_TOKEN = ",('workbench'::charactervarying)::text";
    private static final Pattern NORMALIZED_MESSAGE_LITERAL =
            Pattern.compile("\\('([a-z_]+)'::charactervarying\\)::text");

    private ManagedCoreSchemaContract() {
    }

    /**
     * Project the one known managed evolution back to the immutable adoption
     * representation. Unknown/manual changes are intentionally left untouched,
     * so the pinned baseline hash still rejects them.
     */
    public static String projectConstraintRowToBaseline(String row, boolean workbenchMigrationApplied) {
        if (!workbenchMigrationApplied || row == null || !row.startsWith(MESSAGE_TYPE_ROW_PREFIX)) {
            return row;
        }

        Set<String> actualTypes = normalizedMessageTypes(row);
        if (!actualTypes.equals(lowercase(WORKBENCH_MESSAGE_TYPES))) {
            return row;
        }
        return row.replace(WORKBENCH_NORMALIZED_TOKEN, "");
    }

    private static Set<String> normalizedMessageTypes(String row) {
        Set<String> values = new LinkedHashSet<>();
        Matcher matcher = NORMALIZED_MESSAGE_LITERAL.matcher(row);
        while (matcher.find()) {
            values.add(matcher.group(1));
        }
        return values;
    }

    private static Set<String> lowercase(Set<String> values) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            normalized.add(value.toLowerCase(Locale.ROOT));
        }
        return normalized;
    }
}
