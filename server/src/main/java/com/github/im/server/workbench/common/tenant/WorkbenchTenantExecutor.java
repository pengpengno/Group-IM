package com.github.im.server.workbench.common.tenant;

import com.github.im.server.workbench.common.context.WorkbenchTenantScope;

import java.util.function.Supplier;

/**
 * Explicit tenant boundary for background jobs and non-HTTP execution.
 * Transactions/repository calls must begin inside the supplied operation.
 */
public interface WorkbenchTenantExecutor {

    <T> T execute(WorkbenchTenantScope tenantScope, Supplier<T> operation);

    void execute(WorkbenchTenantScope tenantScope, Runnable operation);
}
