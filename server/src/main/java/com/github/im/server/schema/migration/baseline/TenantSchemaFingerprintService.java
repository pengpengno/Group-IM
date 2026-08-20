package com.github.im.server.schema.migration.baseline;

import com.github.im.server.exception.BusinessException;
import com.github.im.server.schema.migration.support.SchemaNameValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class TenantSchemaFingerprintService {

    private static final List<String> CORE_TABLE_NAMES =
            CoreTenantBaselineContract.CORE_TABLES.stream().sorted().toList();
    private static final List<String> IDENTITY_VIEW_NAMES =
            CoreTenantBaselineContract.IDENTITY_VIEWS.stream().sorted().toList();

    private final DataSource dataSource;
    private final SchemaNameValidator schemaNameValidator;

    public TenantSchemaFingerprintService(DataSource dataSource, SchemaNameValidator schemaNameValidator) {
        this.dataSource = dataSource;
        this.schemaNameValidator = schemaNameValidator;
    }

    public TenantSchemaFingerprint fingerprint(String rawSchemaName, Long companyId) {
        String schemaName = schemaNameValidator.requireTenantSchema(rawSchemaName);
        try (Connection connection = dataSource.getConnection()) {
            Map<String, List<String>> categories = new LinkedHashMap<>();

            Set<String> allTables = loadAllTables(connection, schemaName);
            Set<String> tables = loadCoreTables(connection, schemaName, categories);
            loadCoreColumns(connection, schemaName, categories);
            loadCoreConstraints(connection, schemaName, categories);
            loadCoreIndexes(connection, schemaName, categories);

            Set<String> allViews = loadAllViews(connection, schemaName);
            Set<String> views = loadCoreViews(connection, schemaName, categories);

            Set<String> allSequences = loadAllSequences(connection, schemaName);
            Set<String> sequences = loadCoreSequences(connection, schemaName, categories);

            Map<String, String> hashes = new LinkedHashMap<>();
            categories.forEach((name, rows) -> hashes.put(name, sha256(String.join("\n", rows))));

            String combined = hashes.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .reduce((left, right) -> left + "\n" + right)
                    .orElse("");

            boolean identityViewsValid = views.equals(CoreTenantBaselineContract.IDENTITY_VIEWS)
                    && validateIdentityViews(connection, schemaName, companyId);

            return new TenantSchemaFingerprint(
                    sha256(combined),
                    Map.copyOf(hashes),
                    Set.copyOf(tables),
                    Set.copyOf(views),
                    Set.copyOf(sequences),
                    Set.copyOf(allTables),
                    Set.copyOf(allViews),
                    Set.copyOf(allSequences),
                    identityViewsValid
            );
        } catch (SQLException exception) {
            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "MIGRATION_BASELINE_FINGERPRINT_FAILED",
                    "无法计算 tenant schema fingerprint: " + safeMessage(exception)
            );
        }
    }

    private Set<String> loadAllTables(Connection connection, String schemaName) throws SQLException {
        String sql = """
                SELECT c.relname
                FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = ?
                  AND c.relkind IN ('r', 'p')
                  AND c.relname NOT IN ('flyway_schema_history', 'tenant_schema_metadata')
                ORDER BY c.relname
                """;
        return queryNameSet(connection, sql, schemaName);
    }

    private Set<String> loadCoreTables(
            Connection connection,
            String schemaName,
            Map<String, List<String>> categories
    ) throws SQLException {
        String sql = """
                SELECT c.relname, c.relkind::text
                FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = ?
                  AND c.relkind IN ('r', 'p')
                  AND c.relname IN (%s)
                ORDER BY c.relname
                """.formatted(placeholders(CORE_TABLE_NAMES.size()));
        List<String> rows = queryLines(connection, sql, schemaName, CORE_TABLE_NAMES, 2);
        categories.put("tables", rows);
        return firstColumnNames(rows);
    }

    private void loadCoreColumns(
            Connection connection,
            String schemaName,
            Map<String, List<String>> categories
    ) throws SQLException {
        String sql = """
                SELECT table_row.relname,
                       attribute.attnum,
                       attribute.attname,
                       format_type(attribute.atttypid, attribute.atttypmod),
                       attribute.attnotnull,
                       COALESCE(regexp_replace(pg_get_expr(default_row.adbin, default_row.adrelid), '\\s+', '', 'g'), ''),
                       COALESCE(NULLIF(attribute.attidentity, ''), '-'),
                       COALESCE(NULLIF(attribute.attgenerated, ''), '-')
                FROM pg_class table_row
                JOIN pg_namespace namespace_row ON namespace_row.oid = table_row.relnamespace
                JOIN pg_attribute attribute ON attribute.attrelid = table_row.oid
                LEFT JOIN pg_attrdef default_row
                       ON default_row.adrelid = attribute.attrelid
                      AND default_row.adnum = attribute.attnum
                WHERE namespace_row.nspname = ?
                  AND table_row.relkind IN ('r', 'p')
                  AND table_row.relname IN (%s)
                  AND attribute.attnum > 0
                  AND NOT attribute.attisdropped
                ORDER BY table_row.relname, attribute.attnum
                """.formatted(placeholders(CORE_TABLE_NAMES.size()));
        categories.put("columns", queryLines(connection, sql, schemaName, CORE_TABLE_NAMES, 8));
    }

    private void loadCoreConstraints(
            Connection connection,
            String schemaName,
            Map<String, List<String>> categories
    ) throws SQLException {
        String sql = """
                SELECT table_row.relname,
                       constraint_row.conname,
                       constraint_row.contype::text,
                       COALESCE((
                           SELECT string_agg(attribute.attname, ',' ORDER BY key_row.ordinality)
                           FROM unnest(constraint_row.conkey) WITH ORDINALITY key_row(attnum, ordinality)
                           JOIN pg_attribute attribute
                             ON attribute.attrelid = constraint_row.conrelid
                            AND attribute.attnum = key_row.attnum
                       ), ''),
                       CASE
                           WHEN ref_namespace.nspname = namespace_row.nspname THEN '<tenant>'
                           ELSE COALESCE(ref_namespace.nspname, '')
                       END,
                       COALESCE(ref_table.relname, ''),
                       COALESCE((
                           SELECT string_agg(attribute.attname, ',' ORDER BY key_row.ordinality)
                           FROM unnest(constraint_row.confkey) WITH ORDINALITY key_row(attnum, ordinality)
                           JOIN pg_attribute attribute
                             ON attribute.attrelid = constraint_row.confrelid
                            AND attribute.attnum = key_row.attnum
                       ), ''),
                       CASE
                           WHEN constraint_row.contype = 'c' THEN lower(
                               regexp_replace(
                                   replace(pg_get_constraintdef(constraint_row.oid, true), '"', ''),
                                   '\\s+',
                                   '',
                                   'g'
                               )
                           )
                           ELSE ''
                       END
                FROM pg_constraint constraint_row
                JOIN pg_class table_row ON table_row.oid = constraint_row.conrelid
                JOIN pg_namespace namespace_row ON namespace_row.oid = table_row.relnamespace
                LEFT JOIN pg_class ref_table ON ref_table.oid = constraint_row.confrelid
                LEFT JOIN pg_namespace ref_namespace ON ref_namespace.oid = ref_table.relnamespace
                WHERE namespace_row.nspname = ?
                  AND table_row.relname IN (%s)
                ORDER BY table_row.relname, constraint_row.conname
                """.formatted(placeholders(CORE_TABLE_NAMES.size()));
        categories.put("constraints", queryLines(connection, sql, schemaName, CORE_TABLE_NAMES, 8));
    }

    private void loadCoreIndexes(
            Connection connection,
            String schemaName,
            Map<String, List<String>> categories
    ) throws SQLException {
        String sql = """
                SELECT table_row.relname,
                       index_row.relname,
                       index_meta.indisprimary,
                       index_meta.indisunique,
                       COALESCE((
                           SELECT string_agg(
                               COALESCE(attribute.attname, pg_get_indexdef(index_meta.indexrelid, key_row.ordinality::int, true)),
                               ',' ORDER BY key_row.ordinality
                           )
                           FROM unnest(index_meta.indkey) WITH ORDINALITY key_row(attnum, ordinality)
                           LEFT JOIN pg_attribute attribute
                             ON attribute.attrelid = index_meta.indrelid
                            AND attribute.attnum = key_row.attnum
                           WHERE key_row.ordinality <= index_meta.indnkeyatts
                       ), '')
                FROM pg_index index_meta
                JOIN pg_class table_row ON table_row.oid = index_meta.indrelid
                JOIN pg_class index_row ON index_row.oid = index_meta.indexrelid
                JOIN pg_namespace namespace_row ON namespace_row.oid = table_row.relnamespace
                WHERE namespace_row.nspname = ?
                  AND table_row.relname IN (%s)
                ORDER BY table_row.relname, index_row.relname
                """.formatted(placeholders(CORE_TABLE_NAMES.size()));
        categories.put("indexes", queryLines(connection, sql, schemaName, CORE_TABLE_NAMES, 5));
    }

    private Set<String> loadAllViews(Connection connection, String schemaName) throws SQLException {
        String sql = """
                SELECT view_row.relname
                FROM pg_class view_row
                JOIN pg_namespace namespace_row ON namespace_row.oid = view_row.relnamespace
                WHERE namespace_row.nspname = ?
                  AND view_row.relkind = 'v'
                ORDER BY view_row.relname
                """;
        return queryNameSet(connection, sql, schemaName);
    }

    private Set<String> loadCoreViews(
            Connection connection,
            String schemaName,
            Map<String, List<String>> categories
    ) throws SQLException {
        String sql = """
                SELECT view_row.relname,
                       string_agg(
                           attribute.attname || ':' || format_type(attribute.atttypid, attribute.atttypmod),
                           ',' ORDER BY attribute.attnum
                       )
                FROM pg_class view_row
                JOIN pg_namespace namespace_row ON namespace_row.oid = view_row.relnamespace
                JOIN pg_attribute attribute ON attribute.attrelid = view_row.oid
                WHERE namespace_row.nspname = ?
                  AND view_row.relkind = 'v'
                  AND view_row.relname IN (%s)
                  AND attribute.attnum > 0
                  AND NOT attribute.attisdropped
                GROUP BY view_row.relname
                ORDER BY view_row.relname
                """.formatted(placeholders(IDENTITY_VIEW_NAMES.size()));
        List<String> rows = queryLines(connection, sql, schemaName, IDENTITY_VIEW_NAMES, 2);
        categories.put("views", rows);
        return firstColumnNames(rows);
    }

    private Set<String> loadAllSequences(Connection connection, String schemaName) throws SQLException {
        String sql = """
                SELECT sequence_row.relname
                FROM pg_class sequence_row
                JOIN pg_namespace namespace_row ON namespace_row.oid = sequence_row.relnamespace
                WHERE namespace_row.nspname = ?
                  AND sequence_row.relkind = 'S'
                ORDER BY sequence_row.relname
                """;
        return queryNameSet(connection, sql, schemaName);
    }

    private Set<String> loadCoreSequences(
            Connection connection,
            String schemaName,
            Map<String, List<String>> categories
    ) throws SQLException {
        String sql = """
                SELECT sequence_row.relname,
                       sequence_meta.seqstart,
                       sequence_meta.seqincrement,
                       sequence_meta.seqmin,
                       sequence_meta.seqmax,
                       sequence_meta.seqcache,
                       sequence_meta.seqcycle
                FROM pg_class table_row
                JOIN pg_namespace namespace_row ON namespace_row.oid = table_row.relnamespace
                JOIN pg_attribute attribute
                  ON attribute.attrelid = table_row.oid
                 AND attribute.attnum > 0
                 AND NOT attribute.attisdropped
                JOIN pg_class sequence_row
                  ON sequence_row.oid = to_regclass(
                         pg_get_serial_sequence(
                             format('%%I.%%I', namespace_row.nspname, table_row.relname),
                             attribute.attname
                         )
                     )
                JOIN pg_sequence sequence_meta ON sequence_meta.seqrelid = sequence_row.oid
                WHERE namespace_row.nspname = ?
                  AND table_row.relname IN (%s)
                  AND sequence_row.relkind = 'S'
                ORDER BY sequence_row.relname
                """.formatted(placeholders(CORE_TABLE_NAMES.size()));
        List<String> rows = queryLines(connection, sql, schemaName, CORE_TABLE_NAMES, 7);
        categories.put("sequences", rows);
        return firstColumnNames(rows);
    }

    private Set<String> queryNameSet(Connection connection, String sql, String schemaName) throws SQLException {
        Set<String> names = new LinkedHashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schemaName);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    names.add(resultSet.getString(1));
                }
            }
        }
        return names;
    }

    private List<String> queryLines(
            Connection connection,
            String sql,
            String schemaName,
            List<String> names,
            int columnCount
    ) throws SQLException {
        List<String> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schemaName);
            for (int index = 0; index < names.size(); index++) {
                statement.setString(index + 2, names.get(index));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    StringBuilder line = new StringBuilder();
                    for (int index = 1; index <= columnCount; index++) {
                        if (index > 1) {
                            line.append('|');
                        }
                        String value = resultSet.getString(index);
                        line.append(value == null ? "" : value);
                    }
                    rows.add(line.toString());
                }
            }
        }
        return rows;
    }

    private Set<String> firstColumnNames(List<String> rows) {
        Set<String> names = new LinkedHashSet<>();
        for (String row : rows) {
            int separator = row.indexOf('|');
            names.add(separator < 0 ? row : row.substring(0, separator));
        }
        return names;
    }

    private String placeholders(int size) {
        return String.join(", ", Collections.nCopies(size, "?"));
    }

    private boolean validateIdentityViews(Connection connection, String schemaName, Long companyId) throws SQLException {
        if (companyId == null) {
            return false;
        }
        String qualified = '"' + schemaName + '"';

        long expectedCompany = scalarLong(connection,
                "SELECT count(*) FROM public.company WHERE company_id = ? AND schema_name = ?",
                companyId, schemaName);
        long actualCompany = scalarLong(connection,
                "SELECT count(*) FROM " + qualified + ".company WHERE company_id = ? AND schema_name = ?",
                companyId, schemaName);
        long invalidCompany = scalarLong(connection,
                "SELECT count(*) FROM " + qualified + ".company WHERE company_id <> ? OR schema_name <> ?",
                companyId, schemaName);
        if (expectedCompany != 1 || actualCompany != 1 || invalidCompany != 0) {
            return false;
        }

        long expectedCompanyUsers = scalarLong(connection,
                "SELECT count(*) FROM public.company_user WHERE company_id = ?",
                companyId);
        long actualCompanyUsers = scalarLong(connection,
                "SELECT count(*) FROM " + qualified + ".company_user WHERE company_id = ?",
                companyId);
        long invalidCompanyUsers = scalarLong(connection,
                "SELECT count(*) FROM " + qualified + ".company_user WHERE company_id <> ?",
                companyId);
        if (expectedCompanyUsers != actualCompanyUsers || invalidCompanyUsers != 0) {
            return false;
        }

        long expectedUsers = scalarLong(connection,
                "SELECT count(DISTINCT u.user_id) " +
                        "FROM public.users u JOIN public.company_user cu ON cu.user_id = u.user_id " +
                        "WHERE cu.company_id = ?",
                companyId);
        long actualUsers = scalarLong(connection,
                "SELECT count(*) FROM " + qualified + ".users");
        long invalidUsers = scalarLong(connection,
                "SELECT count(*) FROM " + qualified + ".users tenant_user " +
                        "WHERE NOT EXISTS (SELECT 1 FROM public.company_user cu " +
                        "WHERE cu.company_id = ? AND cu.user_id = tenant_user.user_id)",
                companyId);
        return expectedUsers == actualUsers && invalidUsers == 0;
    }

    private long scalarLong(Connection connection, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) {
                statement.setObject(index + 1, values[index]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Expected scalar result");
                }
                return resultSet.getLong(1);
            }
        }
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private String safeMessage(SQLException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
