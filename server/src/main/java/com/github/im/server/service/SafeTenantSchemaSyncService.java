package com.github.im.server.service;

import com.github.im.server.exception.BusinessException;
import com.github.im.server.model.Company;
import com.github.im.server.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Deliberately conservative schema synchronizer. It never deletes data or alters an existing column.
 * Only missing nullable columns without defaults are eligible for automatic addition.
 */
@Service
@RequiredArgsConstructor
public class SafeTenantSchemaSyncService {
    private static final Set<String> SYSTEM_TABLES = Set.of("company", "company_user", "users");
    private static final String RELEASE_VERSION = "2026.08.14.01";
    private static final String RELEASED_AT = "2026-08-14T02:10:00+08:00";
    private final DataSource dataSource;
    private final CompanyRepository companyRepository;

    public record SchemaStatus(Long companyId, String companyName, String schemaName, String currentFingerprint,
                               String targetFingerprint, String targetVersion, String publishedAt,
                               String status, List<String> differences) {}

    public record SyncResult(String targetVersion, int syncedCount, List<SchemaStatus> tenants) {}

    public List<SchemaStatus> status(List<Long> companyIds) {
        try (Connection connection = dataSource.getConnection()) {
            Map<String, Map<String, ColumnDef>> expected = readManagedStructure(connection, "public");
            String targetFingerprint = fingerprint(expected);
            return targetCompanies(companyIds).stream()
                    .map(company -> inspect(connection, company, expected, targetFingerprint))
                    .toList();
        } catch (SQLException exception) {
            throw new BusinessException("无法读取租户 Schema 状态: " + exception.getMessage());
        }
    }

    /** Preflights every target before issuing any DDL; conflicts leave all tenant data untouched. */
    public SyncResult sync(List<Long> companyIds) {
        List<SchemaStatus> statuses = status(companyIds);
        List<SchemaStatus> conflicts = statuses.stream()
                .filter(status -> !"SYNCED".equals(status.status()) && !"OUTDATED".equals(status.status()))
                .toList();
        if (!conflicts.isEmpty()) {
            String detail = conflicts.stream().map(status -> status.schemaName() + ": " + String.join("; ", status.differences()))
                    .collect(Collectors.joining(" | "));
            throw new BusinessException("Schema 存在冲突，未执行同步: " + detail);
        }
        try (Connection connection = dataSource.getConnection()) {
            Map<String, Map<String, ColumnDef>> expected = readManagedStructure(connection, "public");
            for (Company company : targetCompanies(companyIds)) {
                SchemaStatus current = inspect(connection, company, expected, fingerprint(expected));
                if (!"OUTDATED".equals(current.status())) continue;
                applySafeAdditions(connection, company.getSchemaName(), expected);
            }
        } catch (SQLException exception) {
            throw new BusinessException("Schema 安全同步失败，已停止: " + exception.getMessage());
        }
        List<SchemaStatus> refreshed = status(companyIds);
        return new SyncResult(RELEASE_VERSION, (int) refreshed.stream().filter(status -> "SYNCED".equals(status.status())).count(), refreshed);
    }

    private List<Company> targetCompanies(List<Long> ids) {
        List<Company> companies = ids == null || ids.isEmpty() ? companyRepository.findAll() : companyRepository.findAllById(ids);
        return companies.stream().filter(company -> Boolean.TRUE.equals(company.getActive())).toList();
    }

    private SchemaStatus inspect(Connection connection, Company company, Map<String, Map<String, ColumnDef>> expected, String targetFingerprint) {
        List<String> differences = new ArrayList<>();
        try {
            validateSchema(company.getSchemaName());
            Map<String, Map<String, ColumnDef>> actual = readManagedStructure(connection, company.getSchemaName());
            for (var table : expected.entrySet()) {
                Map<String, ColumnDef> actualColumns = actual.get(table.getKey());
                if (actualColumns == null) { differences.add("缺少表 " + table.getKey() + "（需人工创建，未自动处理）"); continue; }
                for (ColumnDef required : table.getValue().values()) {
                    ColumnDef existing = actualColumns.get(required.name());
                    if (existing == null) {
                        if (required.notNull() || required.defaultExpression() != null) differences.add("缺少字段 " + table.getKey() + "." + required.name() + "，新增可能影响已有数据");
                        else differences.add("可安全补齐字段 " + table.getKey() + "." + required.name());
                    } else if (!required.sameAs(existing)) differences.add("字段定义冲突 " + table.getKey() + "." + required.name());
                }
                for (String extra : actualColumns.keySet()) if (!table.getValue().containsKey(extra)) differences.add("租户额外字段 " + table.getKey() + "." + extra);
            }
            boolean conflict = differences.stream().anyMatch(item -> !item.startsWith("可安全补齐"));
            String state = differences.isEmpty() ? "SYNCED" : conflict ? "CONFLICT" : "OUTDATED";
            return new SchemaStatus(company.getCompanyId(), company.getName(), company.getSchemaName(), fingerprint(actual), targetFingerprint,
                    RELEASE_VERSION, RELEASED_AT, state, differences);
        } catch (Exception exception) {
            return new SchemaStatus(company.getCompanyId(), company.getName(), company.getSchemaName(), null, targetFingerprint,
                    RELEASE_VERSION, RELEASED_AT, "ERROR", List.of(exception.getMessage()));
        }
    }

    private void applySafeAdditions(Connection connection, String schema, Map<String, Map<String, ColumnDef>> expected) throws SQLException {
        Map<String, Map<String, ColumnDef>> actual = readManagedStructure(connection, schema);
        for (var table : expected.entrySet()) {
            Map<String, ColumnDef> actualColumns = actual.get(table.getKey());
            if (actualColumns == null) continue; // preflight reports this as conflict
            for (ColumnDef column : table.getValue().values()) {
                if (!actualColumns.containsKey(column.name()) && !column.notNull() && column.defaultExpression() == null) {
                    try (Statement statement = connection.createStatement()) {
                        statement.executeUpdate("ALTER TABLE " + quote(schema) + "." + quote(table.getKey()) + " ADD COLUMN " + quote(column.name()) + " " + column.type());
                    }
                }
            }
        }
    }

    private Map<String, Map<String, ColumnDef>> readManagedStructure(Connection connection, String schema) throws SQLException {
        Map<String, Map<String, ColumnDef>> result = new TreeMap<>();
        String tables = "SELECT tablename FROM pg_tables WHERE schemaname = ? ORDER BY tablename";
        try (PreparedStatement statement = connection.prepareStatement(tables)) {
            statement.setString(1, schema);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    String table = rows.getString(1);
                    if (SYSTEM_TABLES.contains(table) || table.equals("flyway_schema_history")) continue;
                    result.put(table, readColumns(connection, schema, table));
                }
            }
        }
        return result;
    }

    private Map<String, ColumnDef> readColumns(Connection connection, String schema, String table) throws SQLException {
        String sql = "SELECT a.attname, format_type(a.atttypid, a.atttypmod), a.attnotnull, pg_get_expr(d.adbin, d.adrelid) " +
                "FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid JOIN pg_namespace n ON n.oid=c.relnamespace " +
                "LEFT JOIN pg_attrdef d ON d.adrelid=a.attrelid AND d.adnum=a.attnum WHERE n.nspname=? AND c.relname=? AND a.attnum>0 AND NOT a.attisdropped ORDER BY a.attnum";
        Map<String, ColumnDef> columns = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema); statement.setString(2, table);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) columns.put(rows.getString(1), new ColumnDef(rows.getString(1), rows.getString(2), rows.getBoolean(3), rows.getString(4)));
            }
        }
        return columns;
    }

    private record ColumnDef(String name, String type, boolean notNull, String defaultExpression) {
        boolean sameAs(ColumnDef other) { return type.equals(other.type) && notNull == other.notNull && Objects.equals(defaultExpression, other.defaultExpression); }
    }
    private String fingerprint(Map<String, Map<String, ColumnDef>> structure) {
        try { MessageDigest digest = MessageDigest.getInstance("SHA-256"); return "sha256:" + HexFormat.of().formatHex(digest.digest(structure.toString().getBytes(StandardCharsets.UTF_8))).substring(0, 16); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }
    private void validateSchema(String schema) { if (schema == null || !schema.matches("[A-Za-z0-9_]+")) throw new IllegalArgumentException("非法 schema 名称"); }
    private String quote(String identifier) { return '"' + identifier.replace("\"", "\"\"") + '"'; }
}
