package com.github.im.server.schema.migration.service;

import com.github.im.server.exception.BusinessException;
import com.github.im.server.schema.migration.domain.TenantMigrationPlan;
import com.github.im.server.schema.migration.support.PostgresAdvisoryLock;
import com.github.im.server.schema.migration.support.TenantSchemaInspector;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class TenantMigrationExecutor {
    private final TenantFlywayFactory flywayFactory;
    private final TenantSchemaInspector schemaInspector;
    private final PostgresAdvisoryLock advisoryLock;

    public TenantMigrationExecutor(
            TenantFlywayFactory flywayFactory,
            TenantSchemaInspector schemaInspector,
            PostgresAdvisoryLock advisoryLock
    ) {
        this.flywayFactory = flywayFactory;
        this.schemaInspector = schemaInspector;
        this.advisoryLock = advisoryLock;
    }

    public TenantMigrationPlan plan(String schemaName) {
        TenantSchemaInspector.Inspection inspection = schemaInspector.requireExisting(schemaName);
        try {
            Flyway flyway = flywayFactory.create(inspection.schemaName());
            MigrationInfoService info = flyway.info();
            MigrationInfo current = info.current();
            MigrationInfo[] pending = info.pending();
            List<String> pendingMigrations = Arrays.stream(pending)
                    .map(this::displayName)
                    .toList();
            String currentVersion = versionOf(current);
            String targetVersion = pending.length == 0
                    ? currentVersion
                    : versionOf(pending[pending.length - 1]);

            boolean blocked = inspection.requiresBaseline();
            String blockedReason = blocked
                    ? "tenant schema 非空但没有 Flyway history，必须先通过 #21 的显式 baseline/preflight"
                    : null;

            return new TenantMigrationPlan(
                    inspection.schemaName(),
                    currentVersion,
                    targetVersion,
                    pending.length,
                    pendingMigrations,
                    blocked,
                    blockedReason
            );
        } catch (FlywayException exception) {
            throw flywayFailure("MIGRATION_PLAN_FAILED", "无法生成 tenant migration plan", exception);
        }
    }

    public TenantMigrationPlan apply(String schemaName) {
        TenantSchemaInspector.Inspection inspection = schemaInspector.requireExisting(schemaName);
        if (inspection.requiresBaseline()) {
            throw new BusinessException(HttpStatus.CONFLICT, "MIGRATION_BASELINE_REQUIRED",
                    "tenant schema 非空且没有 Flyway history，拒绝自动 APPLY: " + inspection.schemaName());
        }

        return advisoryLock.withTenantLock(inspection.schemaName(), () -> {
            try {
                Flyway flyway = flywayFactory.create(inspection.schemaName());
                flyway.migrate();
                return plan(inspection.schemaName());
            } catch (FlywayException exception) {
                throw flywayFailure("MIGRATION_APPLY_FAILED", "tenant migration APPLY 失败", exception);
            }
        });
    }

    private String displayName(MigrationInfo migrationInfo) {
        String version = versionOf(migrationInfo);
        return (version == null ? "repeatable" : version) + " - " + migrationInfo.getDescription();
    }

    private String versionOf(MigrationInfo migrationInfo) {
        if (migrationInfo == null || migrationInfo.getVersion() == null) {
            return null;
        }
        return migrationInfo.getVersion().getVersion();
    }

    private BusinessException flywayFailure(String code, String prefix, FlywayException exception) {
        String detail = exception.getMessage();
        if (detail != null && detail.length() > 500) {
            detail = detail.substring(0, 500);
        }
        return new BusinessException(HttpStatus.CONFLICT, code,
                prefix + (detail == null || detail.isBlank() ? "" : ": " + detail));
    }
}
