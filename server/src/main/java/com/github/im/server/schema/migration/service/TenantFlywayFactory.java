package com.github.im.server.schema.migration.service;

import com.github.im.server.schema.migration.support.SchemaNameValidator;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class TenantFlywayFactory {
    private final DataSource dataSource;
    private final SchemaNameValidator schemaNameValidator;
    private final String tenantLocation;

    public TenantFlywayFactory(
            DataSource dataSource,
            SchemaNameValidator schemaNameValidator,
            @Value("${group.schema-migration.tenant-location:classpath:db/migration/tenant}") String tenantLocation
    ) {
        this.dataSource = dataSource;
        this.schemaNameValidator = schemaNameValidator;
        this.tenantLocation = tenantLocation;
    }

    public Flyway create(String rawSchemaName) {
        String schemaName = schemaNameValidator.requireTenantSchema(rawSchemaName);
        return baseConfiguration(schemaName).load();
    }

    /**
     * Explicit baseline factory used only after #21 structural preflight has
     * classified an existing tenant as BASELINE_READY.
     *
     * baselineOnMigrate remains disabled: the caller must invoke Flyway.baseline()
     * deliberately after authorization and fingerprint re-check.
     */
    public Flyway createForExplicitBaseline(String rawSchemaName, String baselineVersion) {
        String schemaName = schemaNameValidator.requireTenantSchema(rawSchemaName);
        return baseConfiguration(schemaName)
                .baselineVersion(MigrationVersion.fromVersion(baselineVersion))
                .baselineDescription("Group-IM reviewed existing-tenant core baseline")
                .load();
    }

    private org.flywaydb.core.api.configuration.FluentConfiguration baseConfiguration(String schemaName) {
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .defaultSchema(schemaName)
                .locations(tenantLocation)
                .createSchemas(false)
                .baselineOnMigrate(false)
                .cleanDisabled(true);
    }
}
