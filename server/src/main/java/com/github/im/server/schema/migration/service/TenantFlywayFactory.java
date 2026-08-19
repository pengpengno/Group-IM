package com.github.im.server.schema.migration.service;

import com.github.im.server.schema.migration.support.SchemaNameValidator;
import org.flywaydb.core.Flyway;
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
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .defaultSchema(schemaName)
                .locations(tenantLocation)
                .createSchemas(false)
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .load();
    }
}
