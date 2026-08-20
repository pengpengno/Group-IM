package com.github.im.server.schema.migration;

import com.github.im.server.exception.BusinessException;
import com.github.im.server.schema.migration.domain.TenantMigrationExecutionResult;
import com.github.im.server.schema.migration.domain.TenantMigrationPlan;
import com.github.im.server.schema.migration.persistence.MigrationControlPlaneRepository;
import com.github.im.server.schema.migration.persistence.TenantCatalogRepository;
import com.github.im.server.schema.migration.service.PublicMigrationBootstrap;
import com.github.im.server.schema.migration.service.TenantFlywayFactory;
import com.github.im.server.schema.migration.service.TenantMigrationExecutor;
import com.github.im.server.schema.migration.support.PostgresAdvisoryLock;
import com.github.im.server.schema.migration.support.SchemaNameValidator;
import com.github.im.server.schema.migration.support.TenantSchemaInspector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class MigrationRuntimeIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private DataSource dataSource;
    private TenantMigrationExecutor executor;
    private TenantCatalogRepository tenantCatalogRepository;
    private MigrationControlPlaneRepository controlPlaneRepository;
    private PublicMigrationBootstrap publicBootstrap;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = dataSource();
        resetDatabase();
        createGlobalIdentityContract();
        createTenant("company_a");
        createTenant("company_b");

        SchemaNameValidator validator = new SchemaNameValidator();
        TenantSchemaInspector inspector = new TenantSchemaInspector(dataSource, validator);
        PostgresAdvisoryLock lock = new PostgresAdvisoryLock(dataSource, validator);
        TenantFlywayFactory flywayFactory = new TenantFlywayFactory(dataSource, validator, "classpath:db/migration/tenant");
        executor = new TenantMigrationExecutor(flywayFactory, inspector, lock);
        tenantCatalogRepository = new TenantCatalogRepository(dataSource);
        controlPlaneRepository = new MigrationControlPlaneRepository(dataSource);
        publicBootstrap = new PublicMigrationBootstrap(dataSource, "classpath:db/migration/public");
    }

    @Test
    void plansAndAppliesOnlySelectedTenantWithoutMutatingOtherTenant() throws Exception {
        publicBootstrap.bootstrap();
        publicBootstrap.bootstrap();

        assertTrue(tableExists("public", "tenant_schema_migration_run"));
        assertTrue(tableExists("public", "tenant_schema_preflight_state"));

        TenantMigrationPlan companyAPlan = executor.plan("company_a");
        assertEquals("2026082002", companyAPlan.targetVersion());
        assertEquals(8, companyAPlan.pendingCount());
        assertFalse(companyAPlan.historyExists());
        assertFalse(tableExists("company_a", "tenant_schema_metadata"));
        assertFalse(tableExists("company_b", "tenant_schema_metadata"));

        TenantMigrationExecutionResult applied = executor.apply("company_a");
        assertEquals("2026082002", applied.currentVersion());
        assertTrue(tableExists("company_a", "tenant_schema_metadata"));
        assertTrue(tableExists("company_a", "wb_task"));
        assertTrue(tableExists("company_a", "flyway_schema_history"));
        assertFalse(tableExists("company_b", "tenant_schema_metadata"));
        assertFalse(tableExists("company_b", "flyway_schema_history"));

        TenantMigrationPlan afterApply = executor.plan("company_a");
        assertEquals("2026082002", afterApply.currentVersion());
        assertEquals(0, afterApply.pendingCount());
        assertTrue(afterApply.historyExists());

        assertEquals(2, tenantCatalogRepository.findAllActive().size());
        assertEquals(1, tenantCatalogRepository.findActiveByIds(List.of(1L)).size());
        assertEquals(0, tenantCatalogRepository.findActiveByIds(List.of(999L)).size());

        assertEquals(0, controlPlaneRepository.findAllStates().size());
    }

    @Test
    void blocksNonEmptyTenantWithoutHistoryUntilExplicitBaselineFlowExists() throws Exception {
        publicBootstrap.bootstrap();
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE company_b.legacy_table(id BIGINT PRIMARY KEY)");
        }

        TenantMigrationPlan plan = executor.plan("company_b");
        assertEquals(1, plan.relationCount());
        assertFalse(plan.historyExists());

        BusinessException exception = assertThrows(BusinessException.class, () -> executor.apply("company_b"));
        assertEquals("MIGRATION_BASELINE_REQUIRED", exception.getErrorCode());
        assertTrue(tableExists("company_b", "legacy_table"));
        assertFalse(tableExists("company_b", "flyway_schema_history"));
        assertFalse(tableExists("company_b", "tenant_schema_metadata"));
    }

    private DataSource dataSource() {
        PGSimpleDataSource source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        return source;
    }

    private void resetDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA public CASCADE");
            statement.execute("CREATE SCHEMA public");
        }
    }

    private void createGlobalIdentityContract() throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE public.company (company_id BIGINT PRIMARY KEY, active BOOLEAN NOT NULL, created_at TIMESTAMP(6), name VARCHAR(255) NOT NULL, schema_name VARCHAR(255) NOT NULL UNIQUE, updated_at TIMESTAMP(6))");
            statement.execute("CREATE TABLE public.users (user_id BIGINT PRIMARY KEY, created_at TIMESTAMP(6), email VARCHAR(255), force_password_change BOOLEAN, password_hash VARCHAR(255), phone_number VARCHAR(255), primary_company_id BIGINT, refresh_token VARCHAR(255), updated_at TIMESTAMP(6), user_status VARCHAR(255), username VARCHAR(255))");
            statement.execute("CREATE TABLE public.company_user (id BIGINT PRIMARY KEY, company_id BIGINT NOT NULL REFERENCES public.company(company_id), status VARCHAR(255), user_id BIGINT NOT NULL REFERENCES public.users(user_id))");
            statement.execute("INSERT INTO public.company(company_id, active, name, schema_name) VALUES (1, TRUE, 'Company A', 'company_a'), (2, TRUE, 'Company B', 'company_b')");
        }
    }

    private void createTenant(String schema) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
        }
    }

    private boolean tableExists(String schema, String table) throws Exception {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.prepareStatement("SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=? AND table_name=?)")) {
            statement.setString(1, schema);
            statement.setString(2, table);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getBoolean(1);
            }
        }
    }
}
