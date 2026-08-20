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
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class TenantSchemaFingerprintService {

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
            Set<String> tables = loadTables(connection, schemaName, categories);
            loadColumns(connection, schemaName, categories);
            loadConstraints(connection, schemaName, categories);
            loadIndexes(connection, schemaName, categories);
            Set<String> views = loadViews(connection, schemaName, categories);
            loadSequences(connection, schemaName, categories);

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

    private Set<String> loadTables(
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
                  AND c.relname NOT IN ('flyway_schema_history', 'tenant_schema_metadata')
                ORDER BY c.relname
                """;
        List<String> rows = new ArrayList<>();
        Set<String> names = new LinkedHashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schemaName);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    names.add(resultSet.getString(1));
                    rows.add(resultSet.getString(1) + "|" + resultSet.getString(2));
                }
            }
        }
        categories.put("tables", rows);
        return names;
    }

    private void loadColumns(
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
                  AND table_row.relname NOT IN ('flyway_schema_history', 'tenant_schema_metadata')
                  AND attribute.attnum > 0
                  AND NOT attribute.attisdropped
                ORDER BY table_row.relname, attribute.attnum
                """;
        categories.put("columns", queryLines(connection, sql, schemaName, 8));
    }

    private void loadConstraints(
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
                       COALESCE(ref_namespace.nspname, ''),
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
                  AND table_row.relname NOT IN ('flyway_schema_history', 'tenant_schema_metadata')
                ORDER BY table_row.relname, constraint_row.conname
                """;
        categories.put("constraints", queryLines(connection, sql, schemaName, 8));
    }

    private void loadIndexes(
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
                  AND table_row.relname NOT IN ('flyway_schema_history', 'tenant_schema_metadata')
                ORDER BY table_row.relname, index_row.relname
                """;
        categories.put("indexes", queryLines(connection, sql, schemaName, 5));
    }

    private Set<String> loadViews(
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
                  AND attribute.attnum > 0
                  AND NOT attribute.attisdropped
                GROUP BY view_row.relname
                ORDER BY view_row.relname
                """;
        List<String> rows = queryLines(connection, sql, schemaName, 2);
        Set<String> names = new LinkedHashSet<>();
        for (String row : rows) {
            int separator = row.indexOf('|');
            names.add(separator < 0 ? row : row.substring(0, separator));
        }
        categories.put("views", rows);
        return names;
    }

    private void loadSequences(
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
                FROM pg_class sequence_row
                JOIN pg_namespace namespace_row ON namespace_row.oid = sequence_row.relnamespace
                JOIN pg_sequence sequence_meta ON sequence_meta.seqrelid = sequence_row.oid
                WHERE namespace_row.nspname = ?
                  AND sequence_row.relkind = 'S'
                ORDER BY sequence_row.relname
                """;
        categories.put("sequences", queryLines(connection, sql, schemaName, 7));
    }

    private List<String> queryLines(
            Connection connection,
            String sql,
            String schemaName,
            int columnCount
    ) throws SQLException {
        List<String> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schemaName);
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
