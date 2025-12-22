package com.github.im.server.config.mult;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

/**
 * 当前租户标识解析器
 * 用于确定当前请求应该使用的租户标识(schema)
 * 
 * 使用TenantContext中的租户信息，确保在线程池环境下也能正确获取租户标识
 */
@Slf4j
public class CurrentTenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver<String> {

    /**
     * 获取当前租户标识
     * 
     * 优先从TenantContext中获取租户标识，如果获取不到再从SecurityContext中解析
     * 这样可以确保在线程池环境下也能正确获取租户标识
     * 
     * @return 租户标识
     */
    @Override
    public String resolveCurrentTenantIdentifier() {
        try {
            // 首先尝试从TenantContext中获取租户标识
            String tenantFromContext = SchemaContext.getCurrentTenant();
            if (tenantFromContext != null && !tenantFromContext.isEmpty()) {
                log.info("从TenantContext获取到租户标识: {}", tenantFromContext);
                return tenantFromContext;
            }

        } catch (Exception e) {
            log.error("解析租户标识时发生错误", e);
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