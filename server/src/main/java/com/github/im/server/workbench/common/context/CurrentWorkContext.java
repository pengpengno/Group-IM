package com.github.im.server.workbench.common.context;

/**
 * Immutable request actor context used by Workbench domain commands and queries.
 * The tenant comes from the authenticated user's current company, never from a
 * client supplied companyId.
 */
public record CurrentWorkContext(
        Long userId,
        String username,
        Long companyId,
        String companyName,
        String schemaName
) {
    public WorkbenchTenantScope tenantScope() {
        return new WorkbenchTenantScope(companyId, schemaName);
    }
}
