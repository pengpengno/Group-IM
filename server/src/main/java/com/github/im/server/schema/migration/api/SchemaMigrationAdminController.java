package com.github.im.server.schema.migration.api;

import com.github.im.server.model.User;
import com.github.im.server.schema.migration.domain.MigrationRunSnapshot;
import com.github.im.server.schema.migration.domain.PublicMigrationBootstrapResult;
import com.github.im.server.schema.migration.domain.TenantSchemaState;
import com.github.im.server.schema.migration.security.MigrationAdminAuthorizer;
import com.github.im.server.schema.migration.service.MigrationRunService;
import com.github.im.server.schema.migration.service.PublicMigrationBootstrap;
import com.github.im.server.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/schema-migrations")
public class SchemaMigrationAdminController {
    private final MigrationAdminAuthorizer adminAuthorizer;
    private final PublicMigrationBootstrap publicBootstrap;
    private final MigrationRunService migrationRunService;

    public SchemaMigrationAdminController(
            MigrationAdminAuthorizer adminAuthorizer,
            PublicMigrationBootstrap publicBootstrap,
            MigrationRunService migrationRunService
    ) {
        this.adminAuthorizer = adminAuthorizer;
        this.publicBootstrap = publicBootstrap;
        this.migrationRunService = migrationRunService;
    }

    @PostMapping("/bootstrap")
    public ResponseEntity<ApiResponse<PublicMigrationBootstrapResult>> bootstrap(
            @AuthenticationPrincipal User user
    ) {
        adminAuthorizer.requireAdmin(user);
        return ResponseEntity.ok(ApiResponse.success("migration control plane 初始化完成", publicBootstrap.bootstrap()));
    }

    @PostMapping("/runs")
    public ResponseEntity<ApiResponse<MigrationRunAccepted>> createRun(
            @Valid @RequestBody MigrationRunRequest request,
            @AuthenticationPrincipal User user
    ) {
        adminAuthorizer.requireAdmin(user);
        MigrationRunAccepted accepted = migrationRunService.createRun(request, user.getUserId());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new ApiResponse<>(HttpStatus.ACCEPTED.value(), "migration run 已进入队列", accepted));
    }

    @GetMapping("/runs/{runId}")
    public ResponseEntity<ApiResponse<MigrationRunSnapshot>> getRun(
            @PathVariable UUID runId,
            @AuthenticationPrincipal User user
    ) {
        adminAuthorizer.requireAdmin(user);
        return ResponseEntity.ok(ApiResponse.success(migrationRunService.getRun(runId)));
    }

    @PostMapping("/runs/{runId}/retry")
    public ResponseEntity<ApiResponse<MigrationRunAccepted>> retryRun(
            @PathVariable UUID runId,
            @AuthenticationPrincipal User user
    ) {
        adminAuthorizer.requireAdmin(user);
        MigrationRunAccepted accepted = migrationRunService.retryFailed(runId, user.getUserId());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new ApiResponse<>(HttpStatus.ACCEPTED.value(), "失败 tenant 已进入重试队列", accepted));
    }

    @GetMapping("/tenants")
    public ResponseEntity<ApiResponse<List<TenantSchemaState>>> listTenantStates(
            @AuthenticationPrincipal User user
    ) {
        adminAuthorizer.requireAdmin(user);
        return ResponseEntity.ok(ApiResponse.success(migrationRunService.listTenantStates()));
    }
}
