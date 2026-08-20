package com.github.im.server.workbench.common.permission;

import com.github.im.server.workbench.common.context.CurrentWorkContext;

public interface WorkbenchPermissionService {

    CurrentWorkContext require(WorkbenchPermission permission);

    boolean isAllowed(WorkbenchPermission permission);
}
