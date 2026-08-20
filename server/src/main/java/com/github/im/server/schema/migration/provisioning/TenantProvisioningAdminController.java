package com.github.im.server.schema.migration.provisioning;

import com.github.im.dto.organization.CompanyDTO;
import com.github.im.server.model.User;
import com.github.im.server.schema.migration.security.MigrationAdminAuthorizer;
import com.github.im.server.service.CompanyService;
import com.github.im.server.web.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tenant-provisioning")
@RequiredArgsConstructor
public class TenantProvisioningAdminController {

    private final MigrationAdminAuthorizer adminAuthorizer;
    private final CompanyService companyService;

    @PostMapping("/companies/{companyId}/retry")
    public ResponseEntity<ApiResponse<CompanyDTO>> retry(
            @PathVariable Long companyId,
            @AuthenticationPrincipal User user
    ) {
        adminAuthorizer.requireAdmin(user);
        return ResponseEntity.ok(ApiResponse.success(
                "tenant provisioning 重试完成",
                companyService.retryProvisioning(companyId)
        ));
    }
}
