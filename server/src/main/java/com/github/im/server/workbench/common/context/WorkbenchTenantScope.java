package com.github.im.server.workbench.common.context;

/**
 * Explicit tenant identity for non-HTTP execution paths.
 */
public record WorkbenchTenantScope(Long companyId, String schemaName) {
}
