package com.github.im.server.workbench.common.permission;

import com.github.im.server.workbench.common.context.CurrentWorkContext;
import com.github.im.server.workbench.common.context.CurrentWorkContextProvider;
import com.github.im.server.workbench.common.integration.OrganizationAdapter;
import com.github.im.server.workbench.common.integration.OrganizationMemberRef;
import com.github.im.server.workbench.common.error.WorkbenchException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultWorkbenchPermissionServiceTest {

    @Test
    void companyMemberReceivesBaselineTaskPermission() {
        DefaultWorkbenchPermissionService service = serviceFor(true);

        CurrentWorkContext context = service.require(WorkbenchPermission.TASK_CREATE);

        assertEquals(42L, context.userId());
        assertEquals(7L, context.companyId());
    }

    @Test
    void privilegedPermissionFailsClosedWithoutPolicy() {
        DefaultWorkbenchPermissionService service = serviceFor(true);

        WorkbenchException exception = assertThrows(
                WorkbenchException.class,
                () -> service.require(WorkbenchPermission.ANNOUNCEMENT_PUBLISH)
        );

        assertEquals("WORKBENCH_PERMISSION_POLICY_MISSING", exception.getErrorCode());
    }

    @Test
    void nonMemberIsDenied() {
        DefaultWorkbenchPermissionService service = serviceFor(false);

        WorkbenchException exception = assertThrows(
                WorkbenchException.class,
                () -> service.require(WorkbenchPermission.VIEW_WORKBENCH)
        );

        assertEquals("WORKBENCH_PERMISSION_DENIED", exception.getErrorCode());
    }

    private DefaultWorkbenchPermissionService serviceFor(boolean member) {
        CurrentWorkContext context = new CurrentWorkContext(42L, "alice", 7L, "Acme", "tenant_a");
        CurrentWorkContextProvider contextProvider = new CurrentWorkContextProvider() {
            @Override
            public Optional<CurrentWorkContext> current() {
                return Optional.of(context);
            }

            @Override
            public CurrentWorkContext require() {
                return context;
            }
        };
        OrganizationAdapter organizationAdapter = new OrganizationAdapter() {
            @Override
            public boolean isActiveMember(Long companyId, Long userId) {
                return member;
            }

            @Override
            public OrganizationMemberRef requireActiveMember(Long companyId, Long userId) {
                if (!member) {
                    throw new IllegalStateException("not a member");
                }
                return new OrganizationMemberRef(userId, "alice", "alice@example.com");
            }
        };
        return new DefaultWorkbenchPermissionService(contextProvider, organizationAdapter);
    }
}
