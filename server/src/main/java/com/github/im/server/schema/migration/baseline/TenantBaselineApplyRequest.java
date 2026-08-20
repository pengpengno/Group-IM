package com.github.im.server.schema.migration.baseline;

import jakarta.validation.constraints.NotBlank;

public record TenantBaselineApplyRequest(
        @NotBlank String expectedFingerprint
) {
}
