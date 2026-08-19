package com.github.im.server.schema.migration.support;

import com.github.im.server.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class TenantSchemaInspector {
    private final DataSource dataSource;
    private final SchemaNameValidator schemaNameValidator;

    public TenantSchemaInspector(DataSource dataSource, SchemaNameValidator schemaNameValidator) {
        this.dataSource = dataSource;
        this.schemaNameValidator = schemaNameValidator;
    }

    public Inspection inspect(String rawSchemaName) {
        String schemaName = schemaNameValidator.requireTenantSchema(rawSchemaName);
        try (Connection connection = dataSource.getConnection()) {
            boolean exists = queryExists(connection,
                    "SELECT EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = ?)", schemaName);
            if (!exists) {
                return new Inspection(schemaName, false, false, false);
            }
            boolean historyExists = queryExists(connection,
                    "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = ? AND table_name = 'flyway_schema_history')",
                    schemaName);
            boolean hasUserTables = queryExists(connection,
                    "SELECT EXISTS (SELECT 1 FROM information_schema.tables " +
                            "WHERE table_schema = ? AND table_type = 'BASE TABLE' AND table_name <> 'flyway_schema_history')",
                    schemaName);
            return new Inspection(schemaName, true, hasUserTables, historyExists);
        } catch (SQLException exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "MIGRATION_SCHEMA_INSPECTION_FAILED",
                    "无法检查 tenant schema 状态: " + exception.getMessage());
        }
    }

    public Inspection requireExisting(String schemaName) {
        Inspection inspection = inspect(schemaName);
        if (!inspection.exists()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "MIGRATION_SCHEMA_NOT_FOUND",
                    "tenant schema 不存在: " + inspection.schemaName());
        }
        return inspection;
    }

    private boolean queryExists(Connection connection, String sql, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getBoolean(1);
            }
        }
    }

    public record Inspection(
            String schemaName,
            boolean exists,
            boolean hasUserTables,
            boolean historyExists
    ) {
        public boolean requiresBaseline() {
            return exists && hasUserTables && !historyExists;
        }
    }
}
