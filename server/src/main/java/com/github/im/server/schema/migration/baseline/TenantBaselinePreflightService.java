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
        Set<String> missingViews = difference(CoreTenantBaselineContract.IDENTITY_VIEWS, fingerprint.views());

        Set<String> extraTables = difference(fingerprint.allTables(), CoreTenantBaselineContract.CORE_TABLES);
        Set<String> extraViews = difference(fingerprint.allViews(), CoreTenantBaselineContract.IDENTITY_VIEWS);
        Set<String> extraSequences = difference(fingerprint.allSequences(), fingerprint.sequences());

        List<String> repairPlan = new ArrayList<>();
        if (!missingTables.isEmpty()) {
            repairPlan.add("缺失 core tables: " + missingTables);
        }
        if (!missingViews.isEmpty()) {
            repairPlan.add("缺失 tenant identity views: " + missingViews);
        }

        // Only an unmanaged no-history tenant must exactly match the adoption
        // inventory. A Flyway-managed tenant is expected to accumulate later
        // immutable migration objects after the 2026081906 core baseline.
        if (!inspection.historyExists()) {
            if (!extraTables.isEmpty()) {
                repairPlan.add("存在 baseline contract 外 tables: " + extraTables + "；无 Flyway history，禁止盲目纳入 baseline。");
            }
            if (!extraViews.isEmpty()) {
                repairPlan.add("存在 baseline contract 外 views: " + extraViews + "；无 Flyway history，先人工确认来源。");
            }
            if (!extraSequences.isEmpty()) {
                repairPlan.add("存在 baseline contract 外 sequences: " + extraSequences + "；无 Flyway history，先确认其 owner/source。");
            }
        }

        if (!missingTables.isEmpty()
                || !missingViews.isEmpty()
                || (!inspection.historyExists()
                    && (!extraTables.isEmpty() || !extraViews.isEmpty() || !extraSequences.isEmpty()))) {
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
            repairPlan.add("tenant identity view 行为不符合当前 company membership；重建 view 前先核对 public.company/company_user/users。");
        }

        TenantBaselineClassification classification = repairPlan.isEmpty()
                ? TenantBaselineClassification.BASELINE_READY
                : TenantBaselineClassification.DRIFTED;

        if (inspection.historyExists() && classification == TenantBaselineClassification.BASELINE_READY) {
            repairPlan.add("已有 Flyway history：core baseline contract 匹配；后续 managed objects 不参与 baseline hash，无需再次 baseline。") ;
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
            case "columns" -> "core column contract drift：逐项核对 type/nullability/default/identity；重点检查 messages.content=TEXT 与 meetings.scheduled_at=timestamp(6)。";
            case "constraints" -> "core constraint contract drift：核对 PK/UNIQUE/FK/CHECK；不要自动 drop/recreate。";
            case "indexes" -> "core index contract drift：核对 #25 baseline 的 PK/UNIQUE backing indexes。";
            case "views" -> "identity view shape drift：核对 company/company_user/users 输出列和隔离行为。";
            case "sequences" -> "core identity sequence drift：核对 2026081906 core tables owned sequences。";
            case "tables" -> "core table contract drift：核对 2026081906 table kind/name。";
            default -> category + " core contract drift";
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
