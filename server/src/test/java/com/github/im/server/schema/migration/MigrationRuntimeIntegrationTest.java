package com.github.im.server.schema.migration;

import com.github.im.server.exception.BusinessException;
import com.github.im.server.schema.migration.api.MigrationRunRequest;
import com.github.im.server.schema.migration.domain.*;
import com.github.im.server.schema.migration.persistence.MigrationRunRepository;
import com.github.im.server.schema.migration.persistence.TenantCatalogRepository;
import com.github.im.server.schema.migration.service.*;
import com.github.im.server.schema.migration.support.PostgresAdvisoryLock;
import com.github.im.server.schema.migration.support.SchemaNameValidator;
import com.github.im.server.schema.migration.support.TenantSchemaInspector;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.core.task.SyncTaskExecutor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class MigrationRuntimeIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static DataSource dataSource;
    static PublicMigrationBootstrap publicBootstrap;
    static TenantMigrationExecutor tenantMigrationExecutor;
    static PostgresAdvisoryLock advisoryLock;
    static MigrationRunService migrationRunService;

    @BeforeAll
    static void setUpRuntime() throws Exception {
        PGSimpleDataSource pgDataSource = new PGSimpleDataSource();
        pgDataSource.setURL(POSTGRES.getJdbcUrl());
        pgDataSource.setUser(POSTGRES.getUsername());
        pgDataSource.setPassword(POSTGRES.getPassword());
        dataSource = pgDataSource;

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE public.company (
                        company_id BIGINT PRIMARY KEY,
                        active BOOLEAN NOT NULL DEFAULT TRUE,
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
            statement.execute("CREATE SCHEMA company_a");
            statement.execute("CREATE SCHEMA company_b");
            statement.execute("CREATE TABLE company_b.legacy_business_table(id BIGINT PRIMARY KEY)");
            statement.execute("INSERT INTO public.company(company_id, name, schema_name, active) VALUES " +
                    "(1, 'Company A', 'company_a', TRUE), " +
                    "(2, 'Company B', 'company_b', TRUE), " +
                    "(3, 'Public Company', 'public', TRUE)");
        }

        SchemaNameValidator validator = new SchemaNameValidator();
        TenantSchemaInspector inspector = new TenantSchemaInspector(dataSource, validator);
        advisoryLock = new PostgresAdvisoryLock(dataSource, validator);
        TenantFlywayFactory flywayFactory = new TenantFlywayFactory(
                dataSource, validator, "classpath:db/migration/tenant"
        );
        tenantMigrationExecutor = new TenantMigrationExecutor(flywayFactory, inspector, advisoryLock);
        publicBootstrap = new PublicMigrationBootstrap(dataSource, "classpath:db/migration/public");

        TenantCatalogRepository catalogRepository = new TenantCatalogRepository(dataSource);
        MigrationRunRepository runRepository = new MigrationRunRepository(dataSource);
        MigrationRunWorker worker = new MigrationRunWorker(
                runRepository,
                tenantMigrationExecutor,
                new SyncTaskExecutor()
        );
        migrationRunService = new MigrationRunService(
                publicBootstrap,
                catalogRepository,
                runRepository,
                worker
        );
    }

    @Test
    void runtimeBootstrapsPlansMigratesAndBlocksUnbaselinedLegacyTenant() {
        PublicMigrationBootstrapResult firstBootstrap = publicBootstrap.bootstrap();
        assertTrue(firstBootstrap.baselineCreated());
        assertEquals("2026082001", firstBootstrap.currentVersion());
        assertTrue(publicBootstrap.isBootstrapped());

        PublicMigrationBootstrapResult secondBootstrap = publicBootstrap.bootstrap();
        assertFalse(secondBootstrap.baselineCreated());
        assertEquals("2026082001", secondBootstrap.currentVersion());

        var planAccepted = migrationRunService.createRun(
                new MigrationRunRequest(MigrationMode.PLAN, List.of(), true),
                9001L
        );
        MigrationRunSnapshot planRun = migrationRunService.getRun(planAccepted.runId());
        assertEquals(MigrationRunStatus.SUCCEEDED, planRun.status());
        assertEquals(2, planRun.items().size(), "public company must not be treated as a tenant target");

        MigrationRunSnapshot.Item companyAPlan = item(planRun, 1L);
        assertEquals(MigrationItemStatus.PLANNED, companyAPlan.status());
        assertEquals(7, companyAPlan.pendingCount());
        assertEquals("2026082001", companyAPlan.targetVersion());
        assertNull(companyAPlan.errorMessage());

        MigrationRunSnapshot.Item companyBPlan = item(planRun, 2L);
        assertEquals(MigrationItemStatus.PLANNED, companyBPlan.status());
        assertNotNull(companyBPlan.errorMessage());
        assertTrue(companyBPlan.errorMessage().contains("baseline"));

        assertFalse(tableExists("company_a", "flyway_schema_history"), "PLAN must not create tenant history");
        assertFalse(tableExists("company_a", "tenant_schema_metadata"), "PLAN must not execute tenant DDL");
        assertFalse(tableExists("company_b", "tenant_schema_metadata"), "PLAN must not alter legacy tenants");

        var applyAccepted = migrationRunService.createRun(
                new MigrationRunRequest(MigrationMode.APPLY, List.of(1L), false),
                9001L
        );
        MigrationRunSnapshot applyRun = migrationRunService.getRun(applyAccepted.runId());
        assertEquals(MigrationRunStatus.SUCCEEDED, applyRun.status());
        MigrationRunSnapshot.Item firstApplyItem = item(applyRun, 1L);
        assertEquals(MigrationItemStatus.SUCCEEDED, firstApplyItem.status());
        assertNull(firstApplyItem.fromVersion());
        assertEquals("2026082001", firstApplyItem.targetVersion());
        assertTrue(tableExists("company_a", "tenant_schema_metadata"));
        assertTrue(tableExists("company_a", "flyway_schema_history"));
        assertTrue(tableExists("company_a", "messages"));
        assertTrue(tableExists("company_a", "meetings"));
        assertFalse(tableExists("company_b", "tenant_schema_metadata"));

        var secondApplyAccepted = migrationRunService.createRun(
                new MigrationRunRequest(MigrationMode.APPLY, List.of(1L), false),
                9001L
        );
        MigrationRunSnapshot secondApplyRun = migrationRunService.getRun(secondApplyAccepted.runId());
        assertEquals(MigrationRunStatus.SUCCEEDED, secondApplyRun.status());
        MigrationRunSnapshot.Item secondApplyItem = item(secondApplyRun, 1L);
        assertEquals(0, secondApplyItem.pendingCount(), "repeat APPLY must be idempotent");
        assertEquals("2026082001", secondApplyItem.fromVersion());
        assertEquals("2026082001", secondApplyItem.targetVersion());

        var blockedAccepted = migrationRunService.createRun(
                new MigrationRunRequest(MigrationMode.APPLY, List.of(2L), false),
                9001L
        );
        MigrationRunSnapshot blockedRun = migrationRunService.getRun(blockedAccepted.runId());
        assertEquals(MigrationRunStatus.FAILED, blockedRun.status());
        assertEquals(MigrationItemStatus.FAILED, item(blockedRun, 2L).status());
        assertTrue(item(blockedRun, 2L).errorMessage().contains("Flyway history"));
        assertTrue(tableExists("company_b", "legacy_business_table"));
        assertFalse(tableExists("company_b", "tenant_schema_metadata"));

        BusinessException locked = assertThrows(BusinessException.class, () ->
                advisoryLock.withTenantLock("company_a", () -> tenantMigrationExecutor.apply("company_a"))
        );
        assertEquals("MIGRATION_TENANT_LOCKED", locked.getErrorCode());
    }

    private MigrationRunSnapshot.Item item(MigrationRunSnapshot run, long companyId) {
        return run.items().stream()
                .filter(item -> item.companyId() != null && item.companyId() == companyId)
                .findFirst()
                .orElseThrow();
    }

    private boolean tableExists(String schemaName, String tableName) {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                     "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=? AND table_name=?)")) {
            statement.setString(1, schemaName);
            statement.setString(2, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getBoolean(1);
            }
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
