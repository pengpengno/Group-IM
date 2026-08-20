package com.github.im.server.schema.migration.provisioning;

import com.github.im.server.exception.BusinessException;
import com.github.im.server.schema.migration.domain.TenantMigrationPlan;
import com.github.im.server.schema.migration.service.TenantFlywayFactory;
import com.github.im.server.schema.migration.service.TenantMigrationExecutor;
import com.github.im.server.schema.migration.support.PostgresAdvisoryLock;
import com.github.im.server.schema.migration.support.SchemaNameValidator;
import com.github.im.server.schema.migration.support.TenantSchemaInspector;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class TenantSchemaProvisionerIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void provisionsEmptyTenantSupportsRetryAndRejectsLegacyClone() throws Exception {
        DataSource dataSource = dataSource();
        createGlobalIdentityContract(dataSource);

        SchemaNameValidator validator = new SchemaNameValidator();
        TenantSchemaInspector inspector = new TenantSchemaInspector(dataSource, validator);
        PostgresAdvisoryLock advisoryLock = new PostgresAdvisoryLock(dataSource, validator);
        TenantFlywayFactory flywayFactory = new TenantFlywayFactory(
                dataSource,
                validator,
                "classpath:db/migration/tenant"
        );
        TenantMigrationExecutor executor = new TenantMigrationExecutor(flywayFactory, inspector, advisoryLock);
        TenantSchemaProvisioner provisioner = new TenantSchemaProvisioner(
                dataSource,
                validator,
                inspector,
                executor
        );

        TenantMigrationPlan first = provisioner.provision("company_new");
        assertEquals("2026082001", first.currentVersion());
        assertEquals(0, first.pendingCount());
        assertTrue(tableExists(dataSource, "company_new", "messages"));
        assertTrue(tableExists(dataSource, "company_new", "flyway_schema_history"));

        TenantMigrationPlan retry = provisioner.provision("company_new");
        assertEquals("2026082001", retry.currentVersion());
        assertEquals(0, retry.pendingCount());

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA legacy_clone");
            statement.execute("CREATE TABLE legacy_clone.messages(id BIGINT PRIMARY KEY)");
        }

        BusinessException legacy = assertThrows(
                BusinessException.class,
                () -> provisioner.provision("legacy_clone")
        );
        assertEquals("MIGRATION_BASELINE_REQUIRED", legacy.getErrorCode());
        assertTrue(tableExists(dataSource, "legacy_clone", "messages"));
    }

    private DataSource dataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        return dataSource;
    }

    private void createGlobalIdentityContract(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE public.company (
                        company_id BIGINT PRIMARY KEY,
                        active BOOLEAN NOT NULL,
                        created_at TIMESTAMP(6) WITHOUT TIME ZONE,
                        name VARCHAR(255) NOT NULL,
                        schema_name VARCHAR(255) NOT NULL UNIQUE,
                        updated_at TIMESTAMP(6) WITHOUT TIME ZONE
                    )
                    """);
            statement.execute("""
                    CREATE TABLE public.users (
                        user_id BIGINT PRIMARY KEY,
                        created_at TIMESTAMP(6) WITHOUT TIME ZONE,
                        email VARCHAR(255),
                        force_password_change BOOLEAN,
                        password_hash VARCHAR(255),
                        phone_number VARCHAR(255),
                        primary_company_id BIGINT,
                        refresh_token VARCHAR(255),
                        updated_at TIMESTAMP(6) WITHOUT TIME ZONE,
                        user_status VARCHAR(255),
                        username VARCHAR(255)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE public.company_user (
                        id BIGINT PRIMARY KEY,
                        company_id BIGINT NOT NULL REFERENCES public.company(company_id),
                        status VARCHAR(255),
                        user_id BIGINT NOT NULL REFERENCES public.users(user_id)
                    )
                    """);
        }
    }

    private boolean tableExists(DataSource dataSource, String schemaName, String tableName) throws Exception {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                     "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=? AND table_name=?)")) {
            statement.setString(1, schemaName);
            statement.setString(2, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getBoolean(1);
            }
        }
    }
}
