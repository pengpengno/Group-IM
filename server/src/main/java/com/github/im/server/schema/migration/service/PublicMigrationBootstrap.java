package com.github.im.server.schema.migration.service;

import com.github.im.server.exception.BusinessException;
import com.github.im.server.schema.migration.domain.PublicMigrationBootstrapResult;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Service
public class PublicMigrationBootstrap {
    private static final MigrationVersion PUBLIC_BASELINE_VERSION = MigrationVersion.fromVersion("2026081900");

    private final DataSource dataSource;
    private final String publicLocation;

    public PublicMigrationBootstrap(
            DataSource dataSource,
            @Value("${group.schema-migration.public-location:classpath:db/migration/public}") String publicLocation
    ) {
        this.dataSource = dataSource;
        this.publicLocation = publicLocation;
    }

    public PublicMigrationBootstrapResult bootstrap() {
        Flyway flyway = createFlyway();
        boolean baselineCreated = false;
        if (!historyExists()) {
            flyway.baseline();
            baselineCreated = true;
        }
        flyway.migrate();
        MigrationInfo current = flyway.info().current();
        return new PublicMigrationBootstrapResult(versionOf(current), baselineCreated);
    }

    public boolean isBootstrapped() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT to_regclass('public.schema_migration_run') IS NOT NULL " +
                             "AND to_regclass('public.schema_migration_run_item') IS NOT NULL " +
                             "AND to_regclass('public.tenant_schema_state') IS NOT NULL");
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() && resultSet.getBoolean(1);
        } catch (SQLException exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "MIGRATION_BOOTSTRAP_CHECK_FAILED",
                    "无法检查 migration control plane: " + exception.getMessage());
        }
    }

    public void requireBootstrapped() {
        if (!isBootstrapped()) {
            throw new BusinessException(HttpStatus.CONFLICT, "MIGRATION_CONTROL_PLANE_NOT_BOOTSTRAPPED",
                    "migration control plane 尚未初始化，请先执行 public bootstrap");
        }
    }

    private Flyway createFlyway() {
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas("public")
                .defaultSchema("public")
                .locations(publicLocation)
                .baselineVersion(PUBLIC_BASELINE_VERSION)
                .baselineDescription("Group-IM public control-plane baseline")
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .load();
    }

    private boolean historyExists() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT to_regclass('public.flyway_schema_history') IS NOT NULL");
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() && resultSet.getBoolean(1);
        } catch (SQLException exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "MIGRATION_PUBLIC_HISTORY_CHECK_FAILED",
                    "无法检查 public Flyway history: " + exception.getMessage());
        }
    }

    private String versionOf(MigrationInfo migrationInfo) {
        if (migrationInfo == null || migrationInfo.getVersion() == null) {
            return null;
        }
        return migrationInfo.getVersion().getVersion();
    }
}
