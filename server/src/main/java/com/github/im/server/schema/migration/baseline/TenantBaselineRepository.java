package com.github.im.server.schema.migration.baseline;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.im.server.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class TenantBaselineRepository {

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    public TenantBaselineRepository(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    public void savePreflight(TenantBaselinePreflightSnapshot snapshot, Long checkedBy) {
        String sql = """
                INSERT INTO public.tenant_schema_preflight_state(
                    company_id, schema_name, classification, baseline_version,
                    history_present, observed_fingerprint, category_hashes,
                    repair_plan, checked_by, checked_at
                ) VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?, ?)
                ON CONFLICT (company_id) DO UPDATE SET
                    schema_name = EXCLUDED.schema_name,
                    classification = EXCLUDED.classification,
                    baseline_version = EXCLUDED.baseline_version,
                    history_present = EXCLUDED.history_present,
                    observed_fingerprint = EXCLUDED.observed_fingerprint,
                    category_hashes = EXCLUDED.category_hashes,
                    repair_plan = EXCLUDED.repair_plan,
                    checked_by = EXCLUDED.checked_by,
                    checked_at = EXCLUDED.checked_at
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, snapshot.companyId());
            statement.setString(2, snapshot.schemaName());
            statement.setString(3, snapshot.classification().name());
            statement.setString(4, snapshot.baselineVersion());
            statement.setBoolean(5, snapshot.historyPresent());
            statement.setString(6, snapshot.observedFingerprint());
            statement.setString(7, objectMapper.writeValueAsString(snapshot.categoryHashes()));
            statement.setString(8, objectMapper.writeValueAsString(snapshot.repairPlan()));
            if (checkedBy == null) {
                statement.setObject(9, null);
            } else {
                statement.setLong(9, checkedBy);
            }
            statement.setTimestamp(10, Timestamp.from(snapshot.checkedAt()));
            statement.executeUpdate();
        } catch (Exception exception) {
            throw persistenceFailure("MIGRATION_BASELINE_PREFLIGHT_SAVE_FAILED", "无法保存 tenant preflight 状态", exception);
        }
    }

    public List<TenantBaselinePreflightSnapshot> listPreflightStates() {
        String sql = """
                SELECT state.company_id,
                       company.name AS company_name,
                       state.schema_name,
                       state.classification,
                       state.baseline_version,
                       state.history_present,
                       state.observed_fingerprint,
                       state.category_hashes::text,
                       state.repair_plan::text,
                       state.checked_at
                FROM public.tenant_schema_preflight_state state
                LEFT JOIN public.company company ON company.company_id = state.company_id
                ORDER BY state.company_id
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<TenantBaselinePreflightSnapshot> result = new ArrayList<>();
            while (resultSet.next()) {
                Map<String, String> categoryHashes = objectMapper.readValue(
                        resultSet.getString(8),
                        new TypeReference<LinkedHashMap<String, String>>() { }
                );
                List<String> repairPlan = objectMapper.readValue(
                        resultSet.getString(9),
                        new TypeReference<List<String>>() { }
                );
                result.add(new TenantBaselinePreflightSnapshot(
                        resultSet.getLong(1),
                        resultSet.getString(2),
                        resultSet.getString(3),
                        TenantBaselineClassification.valueOf(resultSet.getString(4)),
                        resultSet.getString(5),
                        resultSet.getBoolean(6),
                        resultSet.getString(7),
                        Map.copyOf(categoryHashes),
                        List.copyOf(repairPlan),
                        resultSet.getTimestamp(10).toInstant()
                ));
            }
            return result;
        } catch (Exception exception) {
            throw persistenceFailure("MIGRATION_BASELINE_PREFLIGHT_LIST_FAILED", "无法读取 tenant preflight 状态", exception);
        }
    }

    public UUID startAudit(
            Long companyId,
            String schemaName,
            Long operatorUserId,
            String expectedFingerprint,
            String observedFingerprint
    ) {
        UUID auditId = UUID.randomUUID();
        String sql = """
                INSERT INTO public.tenant_schema_baseline_audit(
                    audit_id, company_id, schema_name, operator_user_id,
                    baseline_version, expected_fingerprint, observed_fingerprint,
                    status, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 'ATTEMPTED', NOW())
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, auditId);
            statement.setLong(2, companyId);
            statement.setString(3, schemaName);
            if (operatorUserId == null) {
                statement.setObject(4, null);
            } else {
                statement.setLong(4, operatorUserId);
            }
            statement.setString(5, CoreTenantBaselineContract.BASELINE_VERSION);
            statement.setString(6, expectedFingerprint);
            statement.setString(7, observedFingerprint);
            statement.executeUpdate();
            return auditId;
        } catch (SQLException exception) {
            throw persistenceFailure("MIGRATION_BASELINE_AUDIT_START_FAILED", "无法记录 baseline audit", exception);
        }
    }

    public void completeAudit(UUID auditId, boolean success, String errorMessage) {
        String sql = """
                UPDATE public.tenant_schema_baseline_audit
                SET status = ?, error_message = ?, completed_at = NOW()
                WHERE audit_id = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, success ? "SUCCEEDED" : "FAILED");
            statement.setString(2, truncate(errorMessage));
            statement.setObject(3, auditId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw persistenceFailure("MIGRATION_BASELINE_AUDIT_COMPLETE_FAILED", "无法完成 baseline audit", exception);
        }
    }

    public void markTenantManaged(Long companyId, String schemaName, String currentVersion) {
        String sql = """
                INSERT INTO public.tenant_schema_state(
                    company_id, schema_name, current_version, target_version,
                    status, last_error, updated_at
                ) VALUES (?, ?, ?, ?, 'UP_TO_DATE', NULL, NOW())
                ON CONFLICT (company_id) DO UPDATE SET
                    schema_name = EXCLUDED.schema_name,
                    current_version = EXCLUDED.current_version,
                    target_version = EXCLUDED.target_version,
                    status = 'UP_TO_DATE',
                    last_error = NULL,
                    updated_at = NOW()
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, companyId);
            statement.setString(2, schemaName);
            statement.setString(3, currentVersion);
            statement.setString(4, currentVersion);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw persistenceFailure("MIGRATION_BASELINE_STATE_UPDATE_FAILED", "无法更新 tenant schema state", exception);
        }
    }

    private BusinessException persistenceFailure(String code, String prefix, Exception exception) {
        return new BusinessException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                code,
                prefix + ": " + truncate(exception.getMessage())
        );
    }

    private String truncate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() > 1000 ? value.substring(0, 1000) : value;
    }
}
