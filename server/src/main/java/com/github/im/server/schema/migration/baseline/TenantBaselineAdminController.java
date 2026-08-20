package com.github.im.server.schema.migration.baseline;

import com.github.im.server.model.User;
import com.github.im.server.schema.migration.security.MigrationAdminAuthorizer;
import com.github.im.server.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/schema-migrations/baselines")
@RequiredArgsConstructor
public class TenantBaselineAdminController {

    private final MigrationAdminAuthorizer adminAuthorizer;
    private final TenantBaselinePreflightService preflightService;
    private final ExistingTenantBaselineService baselineService;

    @PostMapping("/preflight")
    public ResponseEntity<ApiResponse<List<TenantBaselinePreflightSnapshot>>> preflight(
            @RequestBody TenantBaselinePreflightRequest request,
            @AuthenticationPrincipal User user
    ) {
        adminAuthorizer.requireAdmin(user);
        return ResponseEntity.ok(ApiResponse.success(
                "existing tenant baseline preflight 完成",
                preflightService.preflight(request, user.getUserId())
        ));
    }

    @GetMapping("/states")
    public ResponseEntity<ApiResponse<List<TenantBaselinePreflightSnapshot>>> states(
            @AuthenticationPrincipal User user
    ) {
        adminAuthorizer.requireAdmin(user);
        return ResponseEntity.ok(ApiResponse.success(preflightService.listStates()));
    }

    @PostMapping("/companies/{companyId}")
    public ResponseEntity<ApiResponse<TenantBaselineResult>> baseline(
            @PathVariable Long companyId,
            @Valid @RequestBody TenantBaselineApplyRequest request,
            @AuthenticationPrincipal User user
    ) {
        adminAuthorizer.requireAdmin(user);
        return ResponseEntity.ok(ApiResponse.success(
                "tenant baseline + verification migration 完成",
                baselineService.baseline(companyId, request, user.getUserId())
        ));
    }
}
