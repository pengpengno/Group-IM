package com.github.im.server.workbench.task;

import com.github.im.server.schema.migration.baseline.CoreTenantBaselineContract;
import com.github.im.server.schema.migration.baseline.ManagedCoreSchemaContractService;
import com.github.im.server.schema.migration.baseline.TenantSchemaFingerprint;
import com.github.im.server.schema.migration.baseline.TenantSchemaFingerprintService;
import com.github.im.server.schema.migration.service.TenantFlywayFactory;
import com.github.im.server.schema.migration.support.SchemaNameValidator;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class TaskMigrationIntegrationTest {

    private static final String SCHEMA = "task_tenant";
    private static final Set<String> TASK_TABLES = Set.of(
            "wb_task",
            "wb_task_assignee",
            "wb_task_comment",
            "wb_task_activity"
    );

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void taskMigrationSurvivesManagedCoreEvolutionWithoutChangingAdoptionFingerprint() throws Exception {
        DataSource dataSource = dataSource();
        createGlobalIdentityAndTenant(dataSource);
        SchemaNameValidator validator = new SchemaNameValidator();
        Flyway flyway = new TenantFlywayFactory(
                dataSource,
                validator,
                "classpath:db/migration/tenant"
        ).create(SCHEMA);

        flyway.migrate();

        assertEquals("2026082003", flyway.info().current().getVersion().getVersion());
        assertTrue(flyway.validateWithResult().validationSuccessful);
        assertTrue(relationNames(dataSource).containsAll(TASK_TABLES));

        ManagedCoreSchemaContractService.Inspection managedCore =
                new ManagedCoreSchemaContractService(dataSource, validator).inspect(SCHEMA);
        assertTrue(managedCore.workbenchMigrationApplied());
        assertTrue(managedCore.valid());
        assertTrue(managedCore.messageTypes().contains("WORKBENCH"));

        TenantSchemaFingerprint fingerprint = new TenantSchemaFingerprintService(dataSource, validator)
                .fingerprint(SCHEMA, 42L, managedCore.workbenchMigrationApplied());
        assertEquals(CoreTenantBaselineContract.expectedCategoryHashes(), fingerprint.categoryHashes());
        assertEquals(CoreTenantBaselineContract.CORE_TABLES, fingerprint.tables());
        assertTrue(fingerprint.allTables().containsAll(TASK_TABLES));
        assertTrue(fingerprint.allSequences().size() > fingerprint.sequences().size());

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO task_tenant.wb_task(title, status, priority, creator_id, progress)
                    VALUES ('valid', 'TODO', 'MEDIUM', 1001, 0)
                    """);
        }

        assertThrows(SQLException.class, () -> {
            try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("""
                        INSERT INTO task_tenant.wb_task(title, status, priority, creator_id, progress)
                        VALUES ('invalid', 'NOT_A_STATUS', 'MEDIUM', 1001, 0)
                        """);
            }
        });
    }

    private DataSource dataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        return dataSource;
    }

    private void createGlobalIdentityAndTenant(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE public.company (company_id BIGINT PRIMARY KEY, active BOOLEAN NOT NULL, created_at TIMESTAMP(6), name VARCHAR(255) NOT NULL, schema_name VARCHAR(255) NOT NULL UNIQUE, updated_at TIMESTAMP(6))");
            statement.execute("CREATE TABLE public.users (user_id BIGINT PRIMARY KEY, created_at TIMESTAMP(6), email VARCHAR(255), force_password_change BOOLEAN, password_hash VARCHAR(255), phone_number VARCHAR(255), primary_company_id BIGINT, refresh_token VARCHAR(255), updated_at TIMESTAMP(6), user_status VARCHAR(255), username VARCHAR(255))");
            statement.execute("CREATE TABLE public.company_user (id BIGINT PRIMARY KEY, company_id BIGINT NOT NULL REFERENCES public.company(company_id), status VARCHAR(255), user_id BIGINT NOT NULL REFERENCES public.users(user_id))");
            statement.execute("CREATE SCHEMA " + SCHEMA);
            statement.execute("INSERT INTO public.company(company_id, active, name, schema_name) VALUES (42, TRUE, 'Task Tenant', 'task_tenant')");
            statement.execute("INSERT INTO public.users(user_id, username) VALUES (1001, 'task-user')");
            statement.execute("INSERT INTO public.company_user(id, company_id, status, user_id) VALUES (5001, 42, 'ACTIVE', 1001)");
        }
    }

    private Set<String> relationNames(DataSource dataSource) throws Exception {
        var names = new java.util.HashSet<String>();
        try (Connection connection = dataSource.getConnection();
             var statement = connection.prepareStatement("""
                     SELECT c.relname
                     FROM pg_class c
                     JOIN pg_namespace n ON n.oid = c.relnamespace
                     WHERE n.nspname = ? AND c.relkind = 'r'
                     """)) {
            statement.setString(1, SCHEMA);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    names.add(resultSet.getString(1));
                }
            }
        }
        return names;
    }
}
