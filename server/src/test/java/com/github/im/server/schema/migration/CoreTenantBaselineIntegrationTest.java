package com.github.im.server.schema.migration;

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class CoreTenantBaselineIntegrationTest {

    private static final String TENANT_SCHEMA = "baseline_tenant";

    private static final Set<String> EXPECTED_CORE_TABLES = Set.of(
            "approval_requests",
            "automation_executions",
            "automation_rules",
            "conversation_bot_configs",
            "conversation_members",
            "conversations",
            "departments",
            "file_resource",
            "friendships",
            "media_file_resource",
            "meeting_participants",
            "meetings",
            "messages",
            "status_updates",
            "system_config_item",
            "upload_chunk_record",
            "user_departments",
            "user_privacy_settings"
    );

    private static final Set<String> EXPECTED_IDENTITY_VIEWS = Set.of(
            "company",
            "company_user",
            "users"
    );

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void emptyTenantMigratesToReviewedCoreSchemaContract() throws Exception {
        DataSource dataSource = dataSource();
        createGlobalIdentityContractAndEmptyTenant(dataSource);

        TenantFlywayFactory flywayFactory = new TenantFlywayFactory(
                dataSource,
                new SchemaNameValidator(),
                "classpath:db/migration/tenant"
        );
        Flyway flyway = flywayFactory.create(TENANT_SCHEMA);
        flyway.migrate();

        assertNotNull(flyway.info().current());
        assertEquals("2026081906", flyway.info().current().getVersion().getVersion());

        assertEquals(EXPECTED_CORE_TABLES, relationNames(dataSource, "r"));
        assertEquals(EXPECTED_IDENTITY_VIEWS, relationNames(dataSource, "v"));
        assertEquals(17, scalarInt(dataSource, """
                SELECT count(*)
                FROM information_schema.sequences
                WHERE sequence_schema = 'baseline_tenant'
                """));
        assertEquals(65, scalarInt(dataSource, """
                SELECT count(*)
                FROM pg_constraint constraint_row
                JOIN pg_namespace namespace_row ON namespace_row.oid = constraint_row.connamespace
                JOIN pg_class table_row ON table_row.oid = constraint_row.conrelid
                WHERE namespace_row.nspname = 'baseline_tenant'
                  AND table_row.relkind = 'r'
                  AND table_row.relname NOT IN ('flyway_schema_history', 'tenant_schema_metadata')
                """));
        assertEquals(26, scalarInt(dataSource, """
                SELECT count(*)
                FROM pg_indexes
                WHERE schemaname = 'baseline_tenant'
                  AND tablename NOT IN ('flyway_schema_history', 'tenant_schema_metadata')
                """));

        assertEquals("text", columnType(dataSource, "messages", "content"));
        assertEquals("timestamp(6) without time zone", columnType(dataSource, "meetings", "scheduled_at"));

        String messageTypeCheck = scalarString(dataSource, """
                SELECT pg_get_constraintdef(constraint_row.oid, true)
                FROM pg_constraint constraint_row
                JOIN pg_class table_row ON table_row.oid = constraint_row.conrelid
                JOIN pg_namespace namespace_row ON namespace_row.oid = table_row.relnamespace
                WHERE namespace_row.nspname = 'baseline_tenant'
                  AND table_row.relname = 'messages'
                  AND constraint_row.conname = 'messages_type_check'
                """);
        assertTrue(messageTypeCheck.contains("BOT_CARD"));

        assertEquals(1, scalarInt(dataSource, "SELECT count(*) FROM baseline_tenant.company"));
        assertEquals("baseline_tenant", scalarString(dataSource,
                "SELECT schema_name FROM baseline_tenant.company"));
        assertEquals(1, scalarInt(dataSource, "SELECT count(*) FROM baseline_tenant.company_user"));
        assertEquals(1, scalarInt(dataSource, "SELECT count(*) FROM baseline_tenant.users"));
        assertEquals("baseline-user", scalarString(dataSource,
                "SELECT username FROM baseline_tenant.users"));
    }

    private DataSource dataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        return dataSource;
    }

    private void createGlobalIdentityContractAndEmptyTenant(DataSource dataSource) throws Exception {
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
            statement.execute("CREATE SCHEMA " + TENANT_SCHEMA);
            statement.execute("""
                    INSERT INTO public.company(company_id, active, name, schema_name)
                    VALUES (42, TRUE, 'Baseline Tenant', 'baseline_tenant')
                    """);
            statement.execute("""
                    INSERT INTO public.company(company_id, active, name, schema_name)
                    VALUES (43, TRUE, 'Other Tenant', 'other_tenant')
                    """);
            statement.execute("""
                    INSERT INTO public.users(user_id, username)
                    VALUES (1001, 'baseline-user'), (1002, 'other-user')
                    """);
            statement.execute("""
                    INSERT INTO public.company_user(id, company_id, status, user_id)
                    VALUES (5001, 42, 'ACTIVE', 1001), (5002, 43, 'ACTIVE', 1002)
                    """);
        }
    }

    private Set<String> relationNames(DataSource dataSource, String relationKind) throws Exception {
        Set<String> names = new HashSet<>();
        try (Connection connection = dataSource.getConnection();
             var statement = connection.prepareStatement("""
                     SELECT relation.relname
                     FROM pg_class relation
                     JOIN pg_namespace namespace_row ON namespace_row.oid = relation.relnamespace
                     WHERE namespace_row.nspname = ?
                       AND relation.relkind::text = ?
                       AND relation.relname NOT IN ('flyway_schema_history', 'tenant_schema_metadata')
                     ORDER BY relation.relname
                     """)) {
            statement.setString(1, TENANT_SCHEMA);
            statement.setString(2, relationKind);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    names.add(resultSet.getString(1));
                }
            }
        }
        return names;
    }

    private String columnType(DataSource dataSource, String tableName, String columnName) throws Exception {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.prepareStatement("""
                     SELECT format_type(attribute.atttypid, attribute.atttypmod)
                     FROM pg_attribute attribute
                     JOIN pg_class table_row ON table_row.oid = attribute.attrelid
                     JOIN pg_namespace namespace_row ON namespace_row.oid = table_row.relnamespace
                     WHERE namespace_row.nspname = ?
                       AND table_row.relname = ?
                       AND attribute.attname = ?
                       AND attribute.attnum > 0
                       AND NOT attribute.attisdropped
                     """)) {
            statement.setString(1, TENANT_SCHEMA);
            statement.setString(2, tableName);
            statement.setString(3, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                return resultSet.getString(1);
            }
        }
    }

    private int scalarInt(DataSource dataSource, String sql) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next());
            return resultSet.getInt(1);
        }
    }

    private String scalarString(DataSource dataSource, String sql) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next());
            return resultSet.getString(1);
        }
    }
}
