package com.github.im.server.config.mult;

import com.github.im.server.service.CompanyService;
import com.github.im.server.service.CompanyUserService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 租户上下文过滤器
 * 在每个HTTP请求开始时设置租户上下文，在请求结束时清除上下文
 * 
 * 该过滤器确保每个请求都能正确设置租户上下文，
 * 并且在请求处理完毕后清理上下文，防止内存泄漏。
 */
@Slf4j
@Component
public class TenantContextFilter implements Filter {

    @Autowired
    private CompanyService companyService;
    
    @Autowired
    private CompanyUserService companyUserService;

    /**
     * 过滤器核心方法，在每个请求处理前后设置和清除租户上下文
     * 
     * @param request  Servlet请求
     * @param response Servlet响应
     * @param chain    过滤器链
     * @throws IOException      IO异常
     * @throws ServletException Servlet异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        // 在请求处理前设置租户上下文
//        setTenantContext();
        
        try {
            // 继续执行过滤器链
            chain.doFilter(request, response);
        } finally {
            // 请求处理完毕后清除租户上下文，防止内存泄漏
            SchemaContext.clear();
        }
    }

    /**
     * 根据当前认证用户设置租户上下文
     */
//    private void setTenantContext() {
//        try {
//            // 从安全上下文中获取当前认证信息
//            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//            if (authentication != null && authentication.getPrincipal() instanceof User user) {
//                Long userId = user.getUserId();
//                // 根据公司ID查询对应的schema名称
//                String schemaName = companyService.getSchemaNameByCompanyId(user.getCurrentLoginCompanyId());
//                TenantContext.setCurrentTenant(schemaName);
//                log.debug("设置租户上下文: {}", schemaName);
//            }
//        } catch (Exception e) {
//            log.error("设置租户上下文时发生错误", e);
//        }
//    }
}