package com.github.im.server.security;

import com.github.im.server.model.User;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

public class CompanyOwnershipSecurityExpressionRoot extends SecurityExpressionRoot implements MethodSecurityExpressionOperations {

    private Object filterObject;
    private Object returnObject;
    private Object target;

    public CompanyOwnershipSecurityExpressionRoot(Authentication authentication) {
        super(authentication);
    }

    /**
     * 检查当前认证用户是否属于指定公司
     * 
     * @param companyId 公司ID
     * @return 如果用户属于该公司则返回true，否则返回false
     */
    public boolean isInCompany(Long companyId) {
        if (companyId == null) {
            return false;
        }
        
        if (getPrincipal() instanceof User) {
            User user = (User) getPrincipal();
            return user.getCurrentCompany().getCompanyId() != null && user.getCurrentCompany().getCompanyId().equals(companyId);
        }
        
        return false;
    }

    @Override
    public void setFilterObject(Object filterObject) {
        this.filterObject = filterObject;
    }

    @Override
    public Object getFilterObject() {
        return filterObject;
    }

    @Override
    public void setReturnObject(Object returnObject) {
        this.returnObject = returnObject;
    }

    @Override
    public Object getReturnObject() {
        return returnObject;
    }

    @Override
    public Object getThis() {
        return target;
    }

    public void setThis(Object target) {
        this.target = target;
    }
}