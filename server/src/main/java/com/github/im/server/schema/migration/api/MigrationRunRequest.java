package com.github.im.server.schema.migration.api;

import com.github.im.server.schema.migration.domain.MigrationMode;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record MigrationRunRequest(
        @NotNull MigrationMode mode,
        List<Long> companyIds,
        boolean allActive
) {
}
