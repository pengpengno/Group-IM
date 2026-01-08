package com.github.im.server.controller;

import com.github.im.server.utils.UserTokenManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserTokenManager userTokenManager;

    @PostMapping("/logout")
    public ResponseEntity<String> logout(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof com.github.im.server.model.User) {
            com.github.im.server.model.User user = (com.github.im.server.model.User) authentication.getPrincipal();
            
            if (user.getCurrentCompany() != null) {
                // 清除用户会话缓存
                userTokenManager.removeAccessToken(user.getUserId());
            }
            
            // 清除安全上下文
            SecurityContextHolder.clearContext();
        }
        
        return ResponseEntity.ok("Logged out successfully");
    }
}