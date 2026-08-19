package com.github.im.server.schema.migration.persistence;

import com.github.im.server.exception.BusinessException;
import com.github.im.server.schema.migration.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class MigrationRunRepository {
    private final JdbcTemplate jdbc;

    public MigrationRunRepository(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Transactional
    public UUID createRun(MigrationMode mode, Long requestedBy, List<TenantTarget> targets) {
        UUID runId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO public.schema_migration_run(run_id, mode, status, requested_by, total_count) " +
                        "VALUES (?, ?, 'QUEUED', ?, ?)",
                runId, mode.name(), requestedBy, targets.size()
        );
        for (TenantTarget target : targets) {
            jdbc.update(
                    "INSERT INTO public.schema_migration_run_item(run_id, company_id, schema_name, status) " +
                            "VALUES (?, ?, ?, 'QUEUED')",
                    runId, target.companyId(), target.schemaName()
            );
            upsertState(target, null, null, TenantSchemaStateStatus.UNKNOWN, runId, null);
        }
        return runId;
    }

    public void markRunRunning(UUID runId) {
        jdbc.update(
                "UPDATE public.schema_migration_run SET status='RUNNING', started_at=COALESCE(started_at, NOW()) WHERE run_id=?",
                runId
        );
    }

    public void markItemRunning(UUID runId, TenantTarget target) {
        jdbc.update(
                "UPDATE public.schema_migration_run_item SET status='RUNNING', started_at=NOW(), error_message=NULL " +
                        "WHERE run_id=? AND company_id=?",
                runId, target.companyId()
        );
        upsertState(target, null, null, TenantSchemaStateStatus.MIGRATING, runId, null);
    }

    public void markItemPlanned(UUID runId, TenantTarget target, TenantMigrationPlan plan, long durationMs) {
        jdbc.update(
                "UPDATE public.schema_migration_run_item SET status='PLANNED', from_version=?, target_version=?, " +
                        "pending_count=?, completed_at=NOW(), duration_ms=?, error_message=? " +
                        "WHERE run_id=? AND company_id=?",
                plan.currentVersion(), plan.targetVersion(), plan.pendingCount(), durationMs,
                plan.blocked() ? plan.blockedReason() : null,
                runId, target.companyId()
        );
        TenantSchemaStateStatus state = plan.blocked()
                ? TenantSchemaStateStatus.DRIFTED
                : plan.pendingCount() > 0 ? TenantSchemaStateStatus.PENDING : TenantSchemaStateStatus.UP_TO_DATE;
        upsertState(target, plan.currentVersion(), plan.targetVersion(), state, runId,
                plan.blocked() ? plan.blockedReason() : null);
    }

    public void markItemSucceeded(
            UUID runId,
            TenantTarget target,
            String fromVersion,
            TenantMigrationPlan result,
            long durationMs
    ) {
        jdbc.update(
                "UPDATE public.schema_migration_run_item SET status='SUCCEEDED', from_version=?, target_version=?, " +
                        "pending_count=?, completed_at=NOW(), duration_ms=?, error_message=NULL " +
                        "WHERE run_id=? AND company_id=?",
                fromVersion, result.targetVersion(), result.pendingCount(), durationMs,
                runId, target.companyId()
        );
        upsertState(target, result.currentVersion(), result.targetVersion(), TenantSchemaStateStatus.UP_TO_DATE, runId, null);
    }

    public void markItemFailed(UUID runId, TenantTarget target, String errorMessage, long durationMs) {
        String safeError = truncate(errorMessage, 1000);
        jdbc.update(
                "UPDATE public.schema_migration_run_item SET status='FAILED', completed_at=NOW(), duration_ms=?, error_message=? " +
                        "WHERE run_id=? AND company_id=?",
                durationMs, safeError, runId, target.companyId()
        );
        upsertState(target, null, null, TenantSchemaStateStatus.FAILED, runId, safeError);
    }

    public void finishRun(UUID runId, MigrationMode mode) {
        int total = count(runId, "1=1");
        int failed = count(runId, "status='FAILED'");
        int success = mode == MigrationMode.PLAN
                ? count(runId, "status='PLANNED'")
                : count(runId, "status='SUCCEEDED'");
        MigrationRunStatus status;
        if (failed == 0) {
            status = MigrationRunStatus.SUCCEEDED;
        } else if (failed < total) {
            status = MigrationRunStatus.PARTIAL_FAILED;
        } else {
            status = MigrationRunStatus.FAILED;
        }
        jdbc.update(
                "UPDATE public.schema_migration_run SET status=?, completed_at=NOW(), total_count=?, success_count=?, failed_count=? " +
                        "WHERE run_id=?",
                status.name(), total, success, failed, runId
        );
    }

    public MigrationRunSnapshot findRun(UUID runId) {
        List<MigrationRunSnapshot> runs = jdbc.query(
                "SELECT run_id, mode, status, requested_by, requested_at, started_at, completed_at, " +
                        "total_count, success_count, failed_count FROM public.schema_migration_run WHERE run_id=?",
                (rs, rowNum) -> new MigrationRunSnapshot(
                        rs.getObject("run_id", UUID.class),
                        MigrationMode.valueOf(rs.getString("mode")),
                        MigrationRunStatus.valueOf(rs.getString("status")),
                        nullableLong(rs, "requested_by"),
                        instant(rs, "requested_at"),
                        instant(rs, "started_at"),
                        instant(rs, "completed_at"),
                        rs.getInt("total_count"),
                        rs.getInt("success_count"),
                        rs.getInt("failed_count"),
                        List.of()
                ),
                runId
        );
        if (runs.isEmpty()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "MIGRATION_RUN_NOT_FOUND", "migration run 不存在: " + runId);
        }
        MigrationRunSnapshot run = runs.getFirst();
        List<MigrationRunSnapshot.Item> items = jdbc.query(
                "SELECT item_id, company_id, schema_name, status, from_version, target_version, pending_count, " +
                        "started_at, completed_at, duration_ms, error_message " +
                        "FROM public.schema_migration_run_item WHERE run_id=? ORDER BY company_id",
                (rs, rowNum) -> new MigrationRunSnapshot.Item(
                        rs.getLong("item_id"),
                        nullableLong(rs, "company_id"),
                        rs.getString("schema_name"),
                        MigrationItemStatus.valueOf(rs.getString("status")),
                        rs.getString("from_version"),
                        rs.getString("target_version"),
                        rs.getInt("pending_count"),
                        instant(rs, "started_at"),
                        instant(rs, "completed_at"),
                        nullableLong(rs, "duration_ms"),
                        rs.getString("error_message")
                ),
                runId
        );
        return new MigrationRunSnapshot(
                run.runId(), run.mode(), run.status(), run.requestedBy(), run.requestedAt(), run.startedAt(), run.completedAt(),
                run.totalCount(), run.successCount(), run.failedCount(), items
        );
    }

    public List<Long> findFailedCompanyIds(UUID runId) {
        return jdbc.query(
                "SELECT company_id FROM public.schema_migration_run_item WHERE run_id=? AND status='FAILED' ORDER BY company_id",
                (rs, rowNum) -> rs.getLong(1),
                runId
        );
    }

    public List<TenantSchemaState> listStates() {
        return jdbc.query(
                "SELECT company_id, schema_name, current_version, target_version, status, last_run_id, last_error, updated_at " +
                        "FROM public.tenant_schema_state ORDER BY company_id",
                (rs, rowNum) -> new TenantSchemaState(
                        nullableLong(rs, "company_id"),
                        rs.getString("schema_name"),
                        rs.getString("current_version"),
                        rs.getString("target_version"),
                        TenantSchemaStateStatus.valueOf(rs.getString("status")),
                        rs.getObject("last_run_id", UUID.class),
                        rs.getString("last_error"),
                        instant(rs, "updated_at")
                )
        );
    }

    private int count(UUID runId, String condition) {
        Integer result = jdbc.queryForObject(
                "SELECT COUNT(*) FROM public.schema_migration_run_item WHERE run_id=? AND " + condition,
                Integer.class,
                runId
        );
        return result == null ? 0 : result;
    }

    private void upsertState(
            TenantTarget target,
            String currentVersion,
            String targetVersion,
            TenantSchemaStateStatus status,
            UUID runId,
            String errorMessage
    ) {
        jdbc.update(
                "INSERT INTO public.tenant_schema_state(company_id, schema_name, current_version, target_version, status, last_run_id, last_error) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT (company_id) DO UPDATE SET schema_name=EXCLUDED.schema_name, " +
                        "current_version=COALESCE(EXCLUDED.current_version, public.tenant_schema_state.current_version), " +
                        "target_version=COALESCE(EXCLUDED.target_version, public.tenant_schema_state.target_version), " +
                        "status=EXCLUDED.status, last_run_id=EXCLUDED.last_run_id, last_error=EXCLUDED.last_error, updated_at=NOW()",
                target.companyId(), target.schemaName(), currentVersion, targetVersion, status.name(), runId, truncate(errorMessage, 1000)
        );
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
