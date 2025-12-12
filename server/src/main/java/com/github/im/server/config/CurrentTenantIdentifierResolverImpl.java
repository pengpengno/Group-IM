package com.github.im.server.config;

import com.github.im.server.model.User;
import com.github.im.server.service.CompanyService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 当前租户标识解析器
 * 用于确定当前请求应该使用的租户标识(schema)
 */
@Slf4j
@Component
public class CurrentTenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver<String> {

    @Autowired
    private CompanyService companyService;

    /**
     * 获取当前租户标识
     * @return 租户标识
     */
    @Override
    public String resolveCurrentTenantIdentifier() {
        // 从安全上下文中获取当前认证信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.debug("当前用户: {}", authentication);
        
        if (authentication != null && authentication.getPrincipal() instanceof User user) {

            // 根据用户所属公司返回对应的schema名称
            if (user.getCurrentLoginCompanyId() != null) {
                // 根据公司ID查询对应的schema名称
                return companyService.getSchemaNameByCompanyId(user.getCurrentLoginCompanyId());
            }
        }
        
        // 默认使用public schema
        return "public";
    }

    /**
     * 验证是否允许无租户标识的操作
     * @return 是否允许无租户标识
     */
    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
    

}