package com.github.im.server.config.security;

import com.github.im.server.config.mult.SchemaContext;
import com.github.im.server.model.User;
import com.github.im.server.utils.UserTokenManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccessTokenRefreshFilter implements Filter {

    private final UserTokenManager userTokenManager;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        try {
            // 继续执行过滤器链
            chain.doFilter(request, response);
            
            // 在请求处理完成后，检查并续期访问令牌
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User user) {
                // 延长accessToken时间
                boolean exists = userTokenManager.extendAccessToken(user.getUserId());
                if (exists) {
                    log.debug("Access token extended for user: {}", user.getUserId());
                    // 设置租户上下文
                    SchemaContext.setCurrentTenant(user.getCurrentSchema());
                } else {
                    log.warn("Access token not found for user: {}", user.getUserId());
                }
            }
        } finally {
            // 清理上下文，防止内存泄漏
            SchemaContext.clear();
        }
    }
}