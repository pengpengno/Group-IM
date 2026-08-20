package com.github.im.server.workbench.common.permission;

import com.github.im.server.workbench.common.context.CurrentWorkContext;
import com.github.im.server.workbench.common.context.CurrentWorkContextProvider;
import com.github.im.server.workbench.common.error.WorkbenchErrorCode;
import com.github.im.server.workbench.common.error.WorkbenchException;
import com.github.im.server.workbench.common.integration.OrganizationAdapter;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

@Service
public class DefaultWorkbenchPermissionService implements WorkbenchPermissionService {

    private static final Set<WorkbenchPermission> COMPANY_MEMBER_BASELINE = EnumSet.of(
            WorkbenchPermission.VIEW_WORKBENCH,
            WorkbenchPermission.TASK_CREATE,
            WorkbenchPermission.TASK_UPDATE,
            WorkbenchPermission.APPROVAL_CREATE,
            WorkbenchPermission.APPROVAL_ACT
    );

    private final CurrentWorkContextProvider contextProvider;
    private final OrganizationAdapter organizationAdapter;

    public DefaultWorkbenchPermissionService(
            CurrentWorkContextProvider contextProvider,
            OrganizationAdapter organizationAdapter
    ) {
        this.contextProvider = contextProvider;
        this.organizationAdapter = organizationAdapter;
    }

    @Override
    public CurrentWorkContext require(WorkbenchPermission permission) {
        CurrentWorkContext context = contextProvider.require();
        if (!COMPANY_MEMBER_BASELINE.contains(permission)) {
            throw WorkbenchException.forbidden(
                    WorkbenchErrorCode.PERMISSION_POLICY_MISSING,
                    "该 Workbench 权限尚未配置策略: " + permission
            );
        }
        if (!organizationAdapter.isActiveMember(context.companyId(), context.userId())) {
            throw WorkbenchException.forbidden(
                    WorkbenchErrorCode.PERMISSION_DENIED,
                    "当前用户无权访问该公司 Workbench"
            );
        }
        return context;
    }

    @Override
    public boolean isAllowed(WorkbenchPermission permission) {
        try {
            require(permission);
            return true;
        } catch (WorkbenchException exception) {
            return false;
        }
    }
}
