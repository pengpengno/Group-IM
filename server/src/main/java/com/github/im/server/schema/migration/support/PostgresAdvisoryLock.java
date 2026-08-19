package com.github.im.server.schema.migration.support;

import com.github.im.server.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Supplier;

@Component
public class PostgresAdvisoryLock {
    private static final String LOCK_NAMESPACE = "group-im-schema-migration";

    private final DataSource dataSource;
    private final SchemaNameValidator schemaNameValidator;

    public PostgresAdvisoryLock(DataSource dataSource, SchemaNameValidator schemaNameValidator) {
        this.dataSource = dataSource;
        this.schemaNameValidator = schemaNameValidator;
    }

    public <T> T withTenantLock(String rawSchemaName, Supplier<T> action) {
        String schemaName = schemaNameValidator.requireTenantSchema(rawSchemaName);
        try (Connection lockConnection = dataSource.getConnection()) {
            if (!tryLock(lockConnection, schemaName)) {
                throw new BusinessException(HttpStatus.CONFLICT, "MIGRATION_TENANT_LOCKED",
                        "tenant schema 正在被其他迁移任务处理: " + schemaName);
            }
            try {
                return action.get();
            } finally {
                unlock(lockConnection, schemaName);
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (SQLException exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "MIGRATION_LOCK_FAILED",
                    "无法获取 tenant migration lock: " + exception.getMessage());
        }
    }

    private boolean tryLock(Connection connection, String schemaName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT pg_try_advisory_lock(hashtext(?), hashtext(?))")) {
            statement.setString(1, LOCK_NAMESPACE);
            statement.setString(2, schemaName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getBoolean(1);
            }
        }
    }

    private void unlock(Connection connection, String schemaName) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT pg_advisory_unlock(hashtext(?), hashtext(?))")) {
            statement.setString(1, LOCK_NAMESPACE);
            statement.setString(2, schemaName);
            statement.execute();
        } catch (SQLException ignored) {
            // Closing the PostgreSQL session releases session-level advisory locks as a final safety net.
        }
    }
}
