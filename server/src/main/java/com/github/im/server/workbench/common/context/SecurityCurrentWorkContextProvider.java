package com.github.im.server.workbench.common.context;

import com.github.im.server.config.mult.SchemaContext;
import com.github.im.server.model.Company;
import com.github.im.server.model.User;
import com.github.im.server.workbench.common.error.WorkbenchErrorCode;
import com.github.im.server.workbench.common.error.WorkbenchException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityCurrentWorkContextProvider implements CurrentWorkContextProvider {

    @Override
    public Optional<CurrentWorkContext> current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User user)) {
            return Optional.empty();
        }
        return Optional.of(resolve(user));
    }

    @Override
    public CurrentWorkContext require() {
        return current().orElseThrow(() -> WorkbenchException.unauthorized("Workbench 需要已认证用户"));
    }

    private CurrentWorkContext resolve(User user) {
        if (user.getUserId() == null) {
            throw WorkbenchException.unauthorized("认证用户缺少 userId");
        }

        Company company = user.getCurrentCompany();
        if (company == null || company.getCompanyId() == null) {
            throw WorkbenchException.forbidden(
                    WorkbenchErrorCode.CURRENT_COMPANY_REQUIRED,
                    "进入 Workbench 前必须选择当前公司"
            );
        }
        if (!Boolean.TRUE.equals(company.getActive())) {
            throw WorkbenchException.forbidden(
                    WorkbenchErrorCode.COMPANY_INACTIVE,
                    "当前公司不可用"
            );
        }

        String schemaName = company.getSchemaName();
        if (schemaName == null || schemaName.isBlank() || "public".equalsIgnoreCase(schemaName)) {
            throw WorkbenchException.badRequest(
                    WorkbenchErrorCode.INVALID_TENANT_SCOPE,
                    "当前公司缺少有效 tenant schema"
            );
        }

        String boundSchema = SchemaContext.getCurrentTenant();
        if (boundSchema != null && !schemaName.equals(boundSchema)) {
            throw WorkbenchException.conflict(
                    WorkbenchErrorCode.TENANT_CONTEXT_MISMATCH,
                    "认证公司与当前 SchemaContext 不一致"
            );
        }

        return new CurrentWorkContext(
                user.getUserId(),
                user.getUsername(),
                company.getCompanyId(),
                company.getName(),
                schemaName
        );
    }
}
