package com.github.im.server.workbench.common.tenant;

import com.github.im.server.config.mult.SchemaContext;
import com.github.im.server.schema.migration.support.SchemaNameValidator;
import com.github.im.server.workbench.common.context.WorkbenchTenantScope;
import com.github.im.server.workbench.common.error.WorkbenchException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SchemaWorkbenchTenantExecutorTest {

    private final SchemaWorkbenchTenantExecutor executor =
            new SchemaWorkbenchTenantExecutor(new SchemaNameValidator());

    @AfterEach
    void cleanup() {
        SchemaContext.clear();
    }

    @Test
    void bindsExplicitTenantAndRestoresPreviousContext() {
        SchemaContext.setCurrentTenant("previous_tenant");

        String observed = executor.execute(
                new WorkbenchTenantScope(7L, "tenant_a"),
                SchemaContext::getCurrentTenant
        );

        assertEquals("tenant_a", observed);
        assertEquals("previous_tenant", SchemaContext.getCurrentTenant());
    }

    @Test
    void clearsContextWhenThereWasNoPreviousTenant() {
        executor.execute(
                new WorkbenchTenantScope(7L, "tenant_a"),
                () -> assertEquals("tenant_a", SchemaContext.getCurrentTenant())
        );

        assertEquals(null, SchemaContext.getCurrentTenant());
    }

    @Test
    void rejectsPublicSchemaForWorkbenchJobs() {
        WorkbenchException exception = assertThrows(
                WorkbenchException.class,
                () -> executor.execute(new WorkbenchTenantScope(7L, "public"), () -> "never")
        );

        assertEquals("WORKBENCH_INVALID_TENANT_SCOPE", exception.getErrorCode());
    }
}
