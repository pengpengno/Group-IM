package com.github.im.server.workbench.common.error;

public enum WorkbenchErrorCode {
    AUTHENTICATION_REQUIRED,
    CURRENT_COMPANY_REQUIRED,
    COMPANY_INACTIVE,
    INVALID_TENANT_SCOPE,
    TENANT_CONTEXT_MISMATCH,
    PERMISSION_DENIED,
    PERMISSION_POLICY_MISSING,
    MEMBER_NOT_FOUND,
    FILE_NOT_AVAILABLE;

    public String code() {
        return "WORKBENCH_" + name();
    }
}
