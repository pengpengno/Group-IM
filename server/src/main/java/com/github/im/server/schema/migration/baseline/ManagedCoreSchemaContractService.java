package com.github.im.server.schema.migration.baseline;

import com.github.im.server.exception.BusinessException;
import com.github.im.server.schema.migration.support.SchemaNameValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ManagedCoreSchemaContractService {

    private static final Pattern SQL_LITERAL = Pattern.compile("'([^']+)'", Pattern.CASE_INSENSITIVE);

    private final DataSource dataSource;
    private final SchemaNameValidator schemaNameValidator;

    public ManagedCoreSchemaContractService(DataSource dataSource, SchemaNameValidator schemaNameValidator) {
        this.dataSource = dataSource;
        this.schemaNameValidator = schemaNameValidator;
    }

    public Inspection inspect(String rawSchemaName) {
        String schemaName = schemaNameValidator.requireTenantSchema(rawSchemaName);
        String qualifiedHistory = quote(schemaName) + ".\"flyway_schema_history\"";

        try (Connection connection = dataSource.getConnection()) {
            boolean historyExists = tableExists(connection, schemaName, "flyway_schema_history");
            if (!historyExists) {
                return new Inspection(false, null, false, Set.of(), false,
                        List.of("tenant 没有 Flyway history；managed-core projection 禁止启用。"));
            }

            String currentVersion = currentVersion(connection, qualifiedHistory);
            boolean workbenchMigrationApplied = migrationApplied(
                    connection,
                    qualifiedHistory,
                    ManagedCoreSchemaContract.WORKBENCH_STORAGE_VERSION
            );
            Set<String> actualMessageTypes = loadMessageTypes(connection, schemaName);
            Set<String> expectedMessageTypes = workbenchMigrationApplied
                    ? ManagedCoreSchemaContract.WORKBENCH_MESSAGE_TYPES
                    : ManagedCoreSchemaContract.BASELINE_MESSAGE_TYPES;

            boolean valid = actualMessageTypes.equals(expectedMessageTypes);
            List<String> problems = valid
                    ? List.of()
                    : List.of(
                            "messages_type_check 与 Flyway history 不一致；expected="
                                    + expectedMessageTypes + ", actual=" + actualMessageTypes
                    );

            return new Inspection(
                    true,
                    currentVersion,
                    workbenchMigrationApplied,
                    Set.copyOf(actualMessageTypes),
                    valid,
                    problems
            );
        } catch (SQLException exception) {
            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "MIGRATION_MANAGED_CORE_INSPECTION_FAILED",
                    "无法验证 managed core contract: " + safeMessage(exception)
            );
        }
    }

    private boolean tableExists(Connection connection, String schemaName, String tableName) throws SQLException {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.tables
                    WHERE table_schema = ? AND table_name = ?
                )
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schemaName);
            statement.setString(2, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getBoolean(1);
            }
        }
    }

    private String currentVersion(Connection connection, String qualifiedHistory) throws SQLException {
        String sql = "SELECT version FROM " + qualifiedHistory
                + " WHERE success = true AND version IS NOT NULL ORDER BY installed_rank DESC LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getString(1) : null;
        }
    }

    private boolean migrationApplied(
            Connection connection,
            String qualifiedHistory,
            String version
    ) throws SQLException {
        String sql = "SELECT EXISTS (SELECT 1 FROM " + qualifiedHistory
                + " WHERE version = ? AND success = true)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, version);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getBoolean(1);
            }
        }
    }

    private Set<String> loadMessageTypes(Connection connection, String schemaName) throws SQLException {
        String sql = """
                SELECT pg_get_constraintdef(constraint_row.oid, true)
                FROM pg_constraint constraint_row
                JOIN pg_class table_row ON table_row.oid = constraint_row.conrelid
                JOIN pg_namespace namespace_row ON namespace_row.oid = table_row.relnamespace
                WHERE namespace_row.nspname = ?
                  AND table_row.relname = 'messages'
                  AND constraint_row.conname = 'messages_type_check'
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schemaName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Set.of();
                }
                String definition = resultSet.getString(1);
                Set<String> values = new LinkedHashSet<>();
                Matcher matcher = SQL_LITERAL.matcher(definition == null ? "" : definition);
                while (matcher.find()) {
                    String value = matcher.group(1);
                    if (value.matches("[A-Z_]+")) {
                        values.add(value);
                    }
                }
                return values;
            }
        }
    }

    private String quote(String identifier) {
        return '"' + identifier.replace("\"", "\"\"") + '"';
    }

    private String safeMessage(SQLException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    public record Inspection(
            boolean historyExists,
            String currentVersion,
            boolean workbenchMigrationApplied,
            Set<String> messageTypes,
            boolean valid,
            List<String> problems
    ) {
    }
}
