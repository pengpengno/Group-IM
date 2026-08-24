package com.github.im.server.schema.migration;

import com.github.im.server.schema.migration.baseline.CoreTenantBaselineContract;
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
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class CoreTenantBaselineIntegrationTest {

    private static final String SCHEMA = "baseline_tenant";
    private static final Set<String> TASK_TABLES = Set.of(
            "wb_task", "wb_task_assignee", "wb_task_comment", "wb_task_activity"
    );

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void emptyTenantMigratesThroughImmutableBaselineAndManagedExtensions() throws Exception {
        DataSource dataSource = dataSource();
        createGlobalIdentityContract(dataSource);
        createTenant(dataSource);

        Flyway flyway = new TenantFlywayFactory(
                dataSource,
                new SchemaNameValidator(),
                "classpath:db/migration/tenant"
        ).create(SCHEMA);

        flyway.migrate();

        assertEquals(CoreTenantBaselineContract.MANAGED_TARGET_VERSION, flyway.info().current().getVersion().getVersion());
        assertTrue(flyway.validateWithResult().validationSuccessful);

        Set<String> relations = relationNames(dataSource);
        assertTrue(relations.containsAll(CoreTenantBaselineContract.CORE_TABLES));
        assertTrue(relations.containsAll(TASK_TABLES));
        assertEquals(22, relations.size());
        assertEquals(Set.of("company", "company_user", "users"), viewNames(dataSource));
        assertEquals(21, identitySequenceCount(dataSource));
        assertEquals(80, constraintCount(dataSource));
        assertEquals(31, primaryOrUniqueBackingIndexCount(dataSource));
        assertEquals("text", columnType(dataSource, "messages", "content"));
        assertEquals("timestamp(6) without time zone", columnType(dataSource, "meetings", "scheduled_at"));
        assertTrue(messageTypeCheck(dataSource).contains("BOT_CARD"));
        assertTrue(messageTypeCheck(dataSource).contains("WORKBENCH"));
        assertTrue(taskStatusCheck(dataSource).contains("IN_PROGRESS"));
        assertFalse(relations.contains("tenant_schema_metadata"));
        assertTrue(tableExists(dataSource, "tenant_schema_metadata"));
        assertTrue(tableExists(dataSource, "flyway_schema_history"));
    }

    private DataSource dataSource() {
        PGSimpleDataSource source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        return source;
    }

    private void createGlobalIdentityContract(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE public.company (company_id BIGINT PRIMARY KEY, active BOOLEAN NOT NULL, created_at TIMESTAMP(6), name VARCHAR(255) NOT NULL, schema_name VARCHAR(255) NOT NULL UNIQUE, updated_at TIMESTAMP(6))");
            statement.execute("CREATE TABLE public.users (user_id BIGINT PRIMARY KEY, created_at TIMESTAMP(6), email VARCHAR(255), force_password_change BOOLEAN, password_hash VARCHAR(255), phone_number VARCHAR(255), primary_company_id BIGINT, refresh_token VARCHAR(255), updated_at TIMESTAMP(6), user_status VARCHAR(255), username VARCHAR(255))");
            statement.execute("CREATE TABLE public.company_user (id BIGINT PRIMARY KEY, company_id BIGINT NOT NULL REFERENCES public.company(company_id), status VARCHAR(255), user_id BIGINT NOT NULL REFERENCES public.users(user_id))");
            statement.execute("INSERT INTO public.company(company_id, active, name, schema_name) VALUES (42, TRUE, 'Baseline Tenant', 'baseline_tenant')");
            statement.execute("INSERT INTO public.users(user_id, username) VALUES (1001, 'baseline-user')");
            statement.execute("INSERT INTO public.company_user(id, company_id, status, user_id) VALUES (5001, 42, 'ACTIVE', 1001)");
        }
    }

    private void createTenant(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + SCHEMA);
        }
    }

    private Set<String> relationNames(DataSource dataSource) throws Exception {
        Set<String> names = new HashSet<>();
        try (Connection connection = dataSource.getConnection();
             var statement = connection.prepareStatement("SELECT c.relname FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname=? AND c.relkind='r' AND c.relname NOT IN ('flyway_schema_history','tenant_schema_metadata')")) {
            statement.setString(1, SCHEMA);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) names.add(rs.getString(1));
            }
        }
        return names;
    }

    private Set<String> viewNames(DataSource dataSource) throws Exception {
        Set<String> names = new HashSet<>();
        try (Connection connection = dataSource.getConnection();
             var statement = connection.prepareStatement("SELECT c.relname FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname=? AND c.relkind='v'")) {
            statement.setString(1, SCHEMA);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) names.add(rs.getString(1));
            }
        }
        return names;
    }

    private int identitySequenceCount(DataSource dataSource) throws Exception {
        return scalarInt(dataSource, "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='" + SCHEMA + "' AND c.relkind='S'");
    }

    private int constraintCount(DataSource dataSource) throws Exception {
        return scalarInt(dataSource, "SELECT count(*) FROM pg_constraint con JOIN pg_class c ON c.oid=con.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='" + SCHEMA + "' AND c.relname NOT IN ('flyway_schema_history','tenant_schema_metadata')");
    }

    private int primaryOrUniqueBackingIndexCount(DataSource dataSource) throws Exception {
        return scalarInt(dataSource, "SELECT count(*) FROM pg_index i JOIN pg_class c ON c.oid=i.indrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='" + SCHEMA + "' AND c.relname NOT IN ('flyway_schema_history','tenant_schema_metadata') AND (i.indisprimary OR i.indisunique)");
    }

    private String columnType(DataSource dataSource, String table, String column) throws Exception {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.prepareStatement("SELECT format_type(a.atttypid,a.atttypmod) FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname=? AND c.relname=? AND a.attname=? AND a.attnum>0 AND NOT a.attisdropped")) {
            statement.setString(1, SCHEMA); statement.setString(2, table); statement.setString(3, column);
            try (ResultSet rs = statement.executeQuery()) { assertTrue(rs.next()); return rs.getString(1); }
        }
    }

    private String messageTypeCheck(DataSource dataSource) throws Exception {
        return checkDefinition(dataSource, "messages", "messages_type_check");
    }

    private String taskStatusCheck(DataSource dataSource) throws Exception {
        return checkDefinition(dataSource, "wb_task", "wb_task_status_check");
    }

    private String checkDefinition(DataSource dataSource, String table, String constraint) throws Exception {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.prepareStatement("SELECT pg_get_constraintdef(con.oid,true) FROM pg_constraint con JOIN pg_class c ON c.oid=con.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname=? AND c.relname=? AND con.conname=?")) {
            statement.setString(1, SCHEMA); statement.setString(2, table); statement.setString(3, constraint);
            try (ResultSet rs = statement.executeQuery()) { assertTrue(rs.next()); return rs.getString(1); }
        }
    }

    private int scalarInt(DataSource dataSource, String sql) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            assertTrue(rs.next()); return rs.getInt(1);
        }
    }

    private boolean tableExists(DataSource dataSource, String table) throws Exception {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.prepareStatement("SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=? AND table_name=?)")) {
            statement.setString(1, SCHEMA); statement.setString(2, table);
            try (ResultSet rs = statement.executeQuery()) { return rs.next() && rs.getBoolean(1); }
        }
    }
}
