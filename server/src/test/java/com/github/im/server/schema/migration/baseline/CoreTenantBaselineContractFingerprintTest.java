package com.github.im.server.schema.migration.baseline;

import com.github.im.server.schema.migration.service.TenantFlywayFactory;
import com.github.im.server.schema.migration.support.SchemaNameValidator;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class CoreTenantBaselineContractFingerprintTest {

    private static final String SCHEMA = "contract_tenant";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void canonicalMigrationsProducePinnedFingerprint() throws Exception {
        DataSource dataSource = dataSource();
        createGlobalIdentityAndTenant(dataSource);

        SchemaNameValidator validator = new SchemaNameValidator();
        TenantFlywayFactory flywayFactory = new TenantFlywayFactory(
                dataSource,
                validator,
                "classpath:db/migration/tenant"
        );
        flywayFactory.create(SCHEMA).migrate();

        TenantSchemaFingerprint fingerprint = new TenantSchemaFingerprintService(dataSource, validator)
                .fingerprint(SCHEMA, 42L);

        assertEquals(CoreTenantBaselineContract.CORE_TABLES, fingerprint.tables());
        assertEquals(CoreTenantBaselineContract.IDENTITY_VIEWS, fingerprint.views());
        assertTrue(fingerprint.identityViewsValid());
        assertEquals(
                CoreTenantBaselineContract.expectedCategoryHashes(),
                fingerprint.categoryHashes(),
                () -> "Pin these canonical category hashes: " + fingerprint.categoryHashes()
        );
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
            statement.execute("CREATE SCHEMA " + SCHEMA);
            statement.execute("""
                    INSERT INTO public.company(company_id, active, name, schema_name)
                    VALUES (42, TRUE, 'Contract Tenant', 'contract_tenant')
                    """);
            statement.execute("INSERT INTO public.users(user_id, username) VALUES (1001, 'contract-user')");
            statement.execute("""
                    INSERT INTO public.company_user(id, company_id, status, user_id)
                    VALUES (5001, 42, 'ACTIVE', 1001)
                    """);
        }
    }
}
