package com.github.im.server.schema.migration.baseline;

import com.github.im.server.exception.BusinessException;
import com.github.im.server.schema.migration.domain.TenantTarget;
import com.github.im.server.schema.migration.persistence.TenantCatalogRepository;
import com.github.im.server.schema.migration.service.PublicMigrationBootstrap;
import com.github.im.server.schema.migration.service.TenantFlywayFactory;
import com.github.im.server.schema.migration.support.PostgresAdvisoryLock;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ExistingTenantBaselineService {

    private final PublicMigrationBootstrap publicBootstrap;
    private final TenantCatalogRepository tenantCatalogRepository;
    private final TenantBaselinePreflightService preflightService;
    private final TenantBaselineRepository baselineRepository;
    private final TenantFlywayFactory flywayFactory;
    private final PostgresAdvisoryLock advisoryLock;

    public ExistingTenantBaselineService(
            PublicMigrationBootstrap publicBootstrap,
            TenantCatalogRepository tenantCatalogRepository,
            TenantBaselinePreflightService preflightService,
            TenantBaselineRepository baselineRepository,
            TenantFlywayFactory flywayFactory,
            PostgresAdvisoryLock advisoryLock
    ) {
        this.publicBootstrap = publicBootstrap;
        this.tenantCatalogRepository = tenantCatalogRepository;
        this.preflightService = preflightService;
        this.baselineRepository = baselineRepository;
        this.flywayFactory = flywayFactory;
        this.advisoryLock = advisoryLock;
    }

    public TenantBaselineResult baseline(
            Long companyId,
            TenantBaselineApplyRequest request,
            Long operatorUserId
    ) {
        publicBootstrap.requireBootstrapped();
        if (request == null || request.expectedFingerprint() == null || request.expectedFingerprint().isBlank()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "MIGRATION_BASELINE_FINGERPRINT_REQUIRED",
                    "显式 baseline 必须携带最近 preflight 返回的 expectedFingerprint"
            );
        }

        TenantTarget target = tenantCatalogRepository.findActiveByIds(List.of(companyId)).getFirst();
        return advisoryLock.withTenantLock(target.schemaName(), () -> baselineLocked(target, request, operatorUserId));
    }

    private TenantBaselineResult baselineLocked(
            TenantTarget target,
            TenantBaselineApplyRequest request,
            Long operatorUserId
    ) {
        TenantBaselinePreflightSnapshot preflight = preflightService.evaluate(target, operatorUserId, true);
        UUID auditId = baselineRepository.startAudit(
                target.companyId(),
                target.schemaName(),
                operatorUserId,
                request.expectedFingerprint(),
                preflight.observedFingerprint()
        );

        try {
            if (preflight.historyPresent()) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "MIGRATION_BASELINE_HISTORY_EXISTS",
                        "tenant 已存在 Flyway history，禁止重复 baseline: " + target.schemaName()
                );
            }
            if (preflight.classification() != TenantBaselineClassification.BASELINE_READY) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "MIGRATION_BASELINE_NOT_READY",
                        "tenant preflight 不是 BASELINE_READY，拒绝 baseline: " + target.schemaName()
                );
            }
            if (!request.expectedFingerprint().equals(preflight.observedFingerprint())) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "MIGRATION_BASELINE_PREFLIGHT_STALE",
                        "tenant 结构在 preflight 后发生变化，请重新执行 preflight"
                );
            }

            Flyway flyway = flywayFactory.createForExplicitBaseline(
                    target.schemaName(),
                    CoreTenantBaselineContract.BASELINE_VERSION
            );
            flyway.baseline();
            flyway.migrate();
            flyway.validate();

            MigrationInfo current = flyway.info().current();
            String currentVersion = current == null || current.getVersion() == null
                    ? null
                    : current.getVersion().getVersion();
            if (!CoreTenantBaselineContract.MANAGED_TARGET_VERSION.equals(currentVersion)
                    || flyway.info().pending().length != 0) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "MIGRATION_BASELINE_POST_MIGRATION_INCOMPLETE",
                        "baseline 后未达到受管 migration target " + CoreTenantBaselineContract.MANAGED_TARGET_VERSION
                );
            }

            TenantBaselinePreflightSnapshot after = preflightService.evaluate(target, operatorUserId, true);
            if (after.classification() != TenantBaselineClassification.BASELINE_READY
                    || !request.expectedFingerprint().equals(after.observedFingerprint())) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "MIGRATION_BASELINE_POSTCHECK_FAILED",
                        "baseline 后 core tenant fingerprint 与授权时不一致"
                );
            }

            baselineRepository.markTenantManaged(target.companyId(), target.schemaName(), currentVersion);
            baselineRepository.completeAudit(auditId, true, null);
            return new TenantBaselineResult(
                    auditId,
                    target.companyId(),
                    target.schemaName(),
                    CoreTenantBaselineContract.BASELINE_VERSION,
                    currentVersion,
                    after.observedFingerprint()
            );
        } catch (RuntimeException exception) {
            completeFailedAudit(auditId, exception);
            throw exception;
        }
    }

    private void completeFailedAudit(UUID auditId, RuntimeException rootCause) {
        try {
            baselineRepository.completeAudit(auditId, false, rootCause.getMessage());
        } catch (RuntimeException auditFailure) {
            rootCause.addSuppressed(auditFailure);
        }
    }
}
