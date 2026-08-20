package com.github.im.server.schema.migration.provisioning;

import com.github.im.server.exception.BusinessException;
import com.github.im.server.schema.migration.domain.TenantMigrationPlan;
import com.github.im.server.schema.migration.service.TenantMigrationExecutor;
import com.github.im.server.schema.migration.support.SchemaNameValidator;
import com.github.im.server.schema.migration.support.TenantSchemaInspector;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Component
public class TenantSchemaProvisioner {

    private final DataSource dataSource;
    private final SchemaNameValidator schemaNameValidator;
    private final TenantSchemaInspector schemaInspector;
    private final TenantMigrationExecutor migrationExecutor;

    public TenantSchemaProvisioner(
            DataSource dataSource,
            SchemaNameValidator schemaNameValidator,
            TenantSchemaInspector schemaInspector,
            TenantMigrationExecutor migrationExecutor
    ) {
        this.dataSource = dataSource;
        this.schemaNameValidator = schemaNameValidator;
        this.schemaInspector = schemaInspector;
        this.migrationExecutor = migrationExecutor;
    }

    /**
     * Provision or resume provisioning for a tenant schema.
     *
     * Retry semantics are intentionally narrow:
     * - missing schema: create it and migrate from empty;
     * - empty schema: migrate it;
     * - schema with Flyway history: resume/apply pending migrations;
     * - non-empty schema without Flyway history: reject and route to #21 preflight/baseline.
     */
    public TenantMigrationPlan provision(String rawSchemaName) {
        String schemaName = schemaNameValidator.requireTenantSchema(rawSchemaName);
        TenantSchemaInspector.Inspection inspection = schemaInspector.inspect(schemaName);

        if (!inspection.exists()) {
            createSchema(schemaName);
            inspection = schemaInspector.requireExisting(schemaName);
        }

        if (inspection.requiresBaseline()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "MIGRATION_BASELINE_REQUIRED",
                    "tenant schema 非空且没有 Flyway history，不能按新租户 provisioning 处理: " + schemaName
            );
        }

        TenantMigrationPlan result = migrationExecutor.apply(schemaName);
        if (result.blocked() || result.pendingCount() != 0 || result.currentVersion() == null) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "MIGRATION_PROVISIONING_INCOMPLETE",
                    "tenant provisioning 未达到最新 migration version: " + schemaName
            );
        }
        return result;
    }

    private void createSchema(String schemaName) {
        String quotedSchema = '"' + schemaName + '"';
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + quotedSchema);
        } catch (SQLException exception) {
            TenantSchemaInspector.Inspection afterFailure = schemaInspector.inspect(schemaName);
            if (afterFailure.exists()) {
                return;
            }
            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "MIGRATION_SCHEMA_CREATE_FAILED",
                    "无法创建 tenant schema " + schemaName + ": " + exception.getMessage()
            );
        }
    }
}
