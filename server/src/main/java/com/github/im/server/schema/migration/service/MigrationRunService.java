package com.github.im.server.schema.migration.service;

import com.github.im.server.exception.BusinessException;
import com.github.im.server.schema.migration.api.MigrationRunAccepted;
import com.github.im.server.schema.migration.api.MigrationRunRequest;
import com.github.im.server.schema.migration.domain.*;
import com.github.im.server.schema.migration.persistence.MigrationRunRepository;
import com.github.im.server.schema.migration.persistence.TenantCatalogRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class MigrationRunService {
    private final PublicMigrationBootstrap publicBootstrap;
    private final TenantCatalogRepository tenantCatalogRepository;
    private final MigrationRunRepository runRepository;
    private final MigrationRunWorker runWorker;

    public MigrationRunService(
            PublicMigrationBootstrap publicBootstrap,
            TenantCatalogRepository tenantCatalogRepository,
            MigrationRunRepository runRepository,
            MigrationRunWorker runWorker
    ) {
        this.publicBootstrap = publicBootstrap;
        this.tenantCatalogRepository = tenantCatalogRepository;
        this.runRepository = runRepository;
        this.runWorker = runWorker;
    }

    public MigrationRunAccepted createRun(MigrationRunRequest request, Long requestedBy) {
        publicBootstrap.requireBootstrapped();
        List<TenantTarget> targets = resolveTargets(request.companyIds(), request.allActive());
        UUID runId = runRepository.createRun(request.mode(), requestedBy, targets);
        runWorker.submit(runId, request.mode(), targets);
        return new MigrationRunAccepted(runId, MigrationRunStatus.QUEUED.name());
    }

    public MigrationRunAccepted retryFailed(UUID sourceRunId, Long requestedBy) {
        publicBootstrap.requireBootstrapped();
        MigrationRunSnapshot source = runRepository.findRun(sourceRunId);
        if (source.mode() != MigrationMode.APPLY) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "MIGRATION_RETRY_PLAN_NOT_ALLOWED",
                    "只有 APPLY run 可以重试失败 tenant");
        }
        List<Long> failedCompanyIds = runRepository.findFailedCompanyIds(sourceRunId);
        if (failedCompanyIds.isEmpty()) {
            throw new BusinessException(HttpStatus.CONFLICT, "MIGRATION_NOTHING_TO_RETRY",
                    "该 migration run 没有失败 tenant");
        }
        List<TenantTarget> targets = tenantCatalogRepository.findActiveByIds(failedCompanyIds);
        UUID runId = runRepository.createRun(MigrationMode.APPLY, requestedBy, targets);
        runWorker.submit(runId, MigrationMode.APPLY, targets);
        return new MigrationRunAccepted(runId, MigrationRunStatus.QUEUED.name());
    }

    public MigrationRunSnapshot getRun(UUID runId) {
        publicBootstrap.requireBootstrapped();
        return runRepository.findRun(runId);
    }

    public List<TenantSchemaState> listTenantStates() {
        publicBootstrap.requireBootstrapped();
        return runRepository.listStates();
    }

    private List<TenantTarget> resolveTargets(List<Long> companyIds, boolean allActive) {
        boolean hasExplicitIds = companyIds != null && !companyIds.isEmpty();
        if (allActive && hasExplicitIds) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "MIGRATION_AMBIGUOUS_SCOPE",
                    "allActive 与 companyIds 不能同时使用");
        }
        if (!allActive && !hasExplicitIds) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "MIGRATION_EMPTY_SCOPE",
                    "必须显式提供 companyIds，或设置 allActive=true");
        }

        List<TenantTarget> targets = allActive
                ? tenantCatalogRepository.findAllActive()
                : tenantCatalogRepository.findActiveByIds(companyIds);
        if (targets.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "MIGRATION_NO_TENANTS",
                    "migration scope 中没有可用 tenant");
        }
        return targets;
    }
}
