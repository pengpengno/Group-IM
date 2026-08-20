package com.github.im.server.schema.migration.baseline;

import com.github.im.server.exception.BusinessException;
import com.github.im.server.schema.migration.domain.TenantTarget;
import com.github.im.server.schema.migration.persistence.TenantCatalogRepository;
import com.github.im.server.schema.migration.service.PublicMigrationBootstrap;
import com.github.im.server.schema.migration.support.TenantSchemaInspector;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TenantBaselinePreflightService {

    private final PublicMigrationBootstrap publicBootstrap;
    private final TenantCatalogRepository tenantCatalogRepository;
    private final TenantSchemaInspector schemaInspector;
    private final TenantSchemaFingerprintService fingerprintService;
    private final TenantBaselineRepository baselineRepository;

    public TenantBaselinePreflightService(
            PublicMigrationBootstrap publicBootstrap,
            TenantCatalogRepository tenantCatalogRepository,
            TenantSchemaInspector schemaInspector,
            TenantSchemaFingerprintService fingerprintService,
            TenantBaselineRepository baselineRepository
    ) {
        this.publicBootstrap = publicBootstrap;
        this.tenantCatalogRepository = tenantCatalogRepository;
        this.schemaInspector = schemaInspector;
        this.fingerprintService = fingerprintService;
        this.baselineRepository = baselineRepository;
    }

    public List<TenantBaselinePreflightSnapshot> preflight(
            TenantBaselinePreflightRequest request,
            Long operatorUserId
    ) {
        publicBootstrap.requireBootstrapped();
        List<TenantTarget> targets = resolveTargets(request);
        List<TenantBaselinePreflightSnapshot> result = new ArrayList<>();
        for (TenantTarget target : targets) {
            result.add(evaluate(target, operatorUserId, true));
        }
        return result;
    }

    public TenantBaselinePreflightSnapshot evaluate(
            TenantTarget target,
            Long operatorUserId,
            boolean persist
    ) {
        TenantBaselinePreflightSnapshot snapshot;
        try {
            TenantSchemaInspector.Inspection inspection = schemaInspector.inspect(target.schemaName());
            if (!inspection.exists()) {
                snapshot = snapshot(
                        target,
                        TenantBaselineClassification.CONFLICT,
                        false,
                        null,
                        Map.of(),
                        List.of("tenant schema 不存在；不要 baseline。确认 company.schema_name 或按新租户 provisioning 重建。")
                );
            } else {
                TenantSchemaFingerprint fingerprint = fingerprintService.fingerprint(
                        target.schemaName(),
                        target.companyId()
                );
                snapshot = classify(target, inspection, fingerprint);
            }
        } catch (Exception exception) {
            snapshot = snapshot(
                    target,
                    TenantBaselineClassification.ERROR,
                    false,
                    null,
                    Map.of(),
                    List.of("preflight 执行失败：" + safeMessage(exception))
            );
        }

        if (persist) {
            baselineRepository.savePreflight(snapshot, operatorUserId);
        }
        return snapshot;
    }

    public List<TenantBaselinePreflightSnapshot> listStates() {
        publicBootstrap.requireBootstrapped();
        return baselineRepository.listPreflightStates();
    }

    private TenantBaselinePreflightSnapshot classify(
            TenantTarget target,
            TenantSchemaInspector.Inspection inspection,
            TenantSchemaFingerprint fingerprint
    ) {
        Set<String> missingTables = difference(CoreTenantBaselineContract.CORE_TABLES, fingerprint.tables());
        Set<String> extraTables = difference(fingerprint.tables(), CoreTenantBaselineContract.CORE_TABLES);
        Set<String> missingViews = difference(CoreTenantBaselineContract.IDENTITY_VIEWS, fingerprint.views());
        Set<String> extraViews = difference(fingerprint.views(), CoreTenantBaselineContract.IDENTITY_VIEWS);

        List<String> repairPlan = new ArrayList<>();
        if (!missingTables.isEmpty()) {
            repairPlan.add("缺失 core tables: " + missingTables);
        }
        if (!extraTables.isEmpty()) {
            repairPlan.add("存在 baseline contract 外 tables: " + extraTables + "；先确认是否为合法后续业务表，禁止盲删。");
        }
        if (!missingViews.isEmpty()) {
            repairPlan.add("缺失 tenant identity views: " + missingViews);
        }
        if (!extraViews.isEmpty()) {
            repairPlan.add("存在 baseline contract 外 views: " + extraViews + "；先人工确认来源。");
        }

        if (!missingTables.isEmpty() || !extraTables.isEmpty() || !missingViews.isEmpty() || !extraViews.isEmpty()) {
            return snapshot(
                    target,
                    TenantBaselineClassification.CONFLICT,
                    inspection.historyExists(),
                    fingerprint.fingerprint(),
                    fingerprint.categoryHashes(),
                    repairPlan
            );
        }

        Map<String, String> expected = CoreTenantBaselineContract.expectedCategoryHashes();
        for (String category : List.of("columns", "constraints", "indexes", "views", "sequences", "tables")) {
            String observed = fingerprint.categoryHashes().get(category);
            String required = expected.get(category);
            if (required != null && !required.equals(observed)) {
                repairPlan.add(repairHint(category));
            }
        }
        if (!fingerprint.identityViewsValid()) {
            repairPlan.add("tenant identity view 行为不符合当前 company membership；重建 view 前先核对 public.company/company_user/users。") ;
        }

        TenantBaselineClassification classification = repairPlan.isEmpty()
                ? TenantBaselineClassification.BASELINE_READY
                : TenantBaselineClassification.DRIFTED;

        if (inspection.historyExists() && classification == TenantBaselineClassification.BASELINE_READY) {
            repairPlan.add("已有 Flyway history：结构匹配 baseline contract，无需再次 baseline；继续使用正常 migration APPLY。");
        }

        return snapshot(
                target,
                classification,
                inspection.historyExists(),
                fingerprint.fingerprint(),
                fingerprint.categoryHashes(),
                repairPlan
        );
    }

    private List<TenantTarget> resolveTargets(TenantBaselinePreflightRequest request) {
        List<Long> ids = request.companyIds() == null ? List.of() : request.companyIds();
        if (request.allActive() && !ids.isEmpty()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "MIGRATION_INVALID_COMPANY_SCOPE",
                    "allActive 与 companyIds 不能同时使用"
            );
        }
        if (request.allActive()) {
            List<TenantTarget> targets = tenantCatalogRepository.findAllActive();
            if (targets.isEmpty()) {
                throw new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "MIGRATION_EMPTY_COMPANY_SCOPE",
                        "没有可 preflight 的 active tenant"
                );
            }
            return targets;
        }
        if (ids.isEmpty()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "MIGRATION_EMPTY_COMPANY_SCOPE",
                    "必须指定 companyIds 或 allActive=true"
            );
        }
        return tenantCatalogRepository.findActiveByIds(new ArrayList<>(new LinkedHashSet<>(ids)));
    }

    private TenantBaselinePreflightSnapshot snapshot(
            TenantTarget target,
            TenantBaselineClassification classification,
            boolean historyPresent,
            String fingerprint,
            Map<String, String> categoryHashes,
            List<String> repairPlan
    ) {
        return new TenantBaselinePreflightSnapshot(
                target.companyId(),
                target.companyName(),
                target.schemaName(),
                classification,
                CoreTenantBaselineContract.BASELINE_VERSION,
                historyPresent,
                fingerprint,
                Map.copyOf(categoryHashes),
                List.copyOf(repairPlan),
                Instant.now()
        );
    }

    private Set<String> difference(Set<String> left, Set<String> right) {
        Set<String> result = new LinkedHashSet<>(left);
        result.removeAll(right);
        return result;
    }

    private String repairHint(String category) {
        return switch (category) {
            case "columns" -> "column contract drift：逐项核对 type/nullability/default/identity；重点检查 messages.content=TEXT 与 meetings.scheduled_at=timestamp(6)。";
            case "constraints" -> "constraint contract drift：核对 PK/UNIQUE/FK/CHECK；不要自动 drop/recreate，先确认数据是否满足 canonical constraint。";
            case "indexes" -> "index contract drift：核对 #25 baseline 的 PK/UNIQUE backing indexes；禁止按 public 当前结构覆盖。";
            case "views" -> "view shape drift：核对 company/company_user/users 输出列；view predicate 使用行为校验，不要求 legacy 文本完全一致。";
            case "sequences" -> "identity sequence drift：核对 17 个 identity sequence 及 start/increment/min/max/cache/cycle。";
            case "tables" -> "table contract drift：核对 core table kind/name。";
            default -> category + " contract drift";
        };
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
