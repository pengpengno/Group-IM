package com.github.im.server.workbench.common.tenant;

import com.github.im.server.exception.BusinessException;
import com.github.im.server.schema.migration.support.SchemaNameValidator;
import com.github.im.server.util.SchemaSwitcher;
import com.github.im.server.workbench.common.context.WorkbenchTenantScope;
import com.github.im.server.workbench.common.error.WorkbenchErrorCode;
import com.github.im.server.workbench.common.error.WorkbenchException;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.Supplier;

@Component
public class SchemaWorkbenchTenantExecutor implements WorkbenchTenantExecutor {

    private final SchemaNameValidator schemaNameValidator;

    public SchemaWorkbenchTenantExecutor(SchemaNameValidator schemaNameValidator) {
        this.schemaNameValidator = schemaNameValidator;
    }

    @Override
    public <T> T execute(WorkbenchTenantScope tenantScope, Supplier<T> operation) {
        Objects.requireNonNull(operation, "operation");
        String schemaName = validateScope(tenantScope);
        return SchemaSwitcher.executeInSchema(schemaName, operation);
    }

    @Override
    public void execute(WorkbenchTenantScope tenantScope, Runnable operation) {
        Objects.requireNonNull(operation, "operation");
        String schemaName = validateScope(tenantScope);
        SchemaSwitcher.executeInSchema(schemaName, operation);
    }

    private String validateScope(WorkbenchTenantScope tenantScope) {
        if (tenantScope == null || tenantScope.companyId() == null || tenantScope.companyId() <= 0) {
            throw WorkbenchException.badRequest(
                    WorkbenchErrorCode.INVALID_TENANT_SCOPE,
                    "Workbench tenant scope 缺少有效 companyId"
            );
        }
        try {
            return schemaNameValidator.requireTenantSchema(tenantScope.schemaName());
        } catch (BusinessException exception) {
            throw WorkbenchException.badRequest(
                    WorkbenchErrorCode.INVALID_TENANT_SCOPE,
                    "Workbench tenant scope 缺少有效 schemaName"
            );
        }
    }
}
