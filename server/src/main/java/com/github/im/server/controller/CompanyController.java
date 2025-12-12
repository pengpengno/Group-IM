package com.github.im.server.controller;

import com.github.im.dto.user.SwitchCompanyRequest;
import com.github.im.dto.user.UserInfo;
import com.github.im.server.model.User;
import com.github.im.server.service.UserService;
import com.github.im.server.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
public class CompanyController {
    
    @Autowired
    private final UserService userService;
    
    @Autowired
    private final JwtUtil jwtUtil;

    /**
     * 切换当前登录公司
     * @param switchCompanyRequest 包含目标公司ID的请求
     * @return 更新后的用户信息和新的JWT令牌
     */
    @PostMapping("/switch")
    public ResponseEntity<UserInfo> switchCompany(@RequestBody SwitchCompanyRequest switchCompanyRequest) {
        // 获取当前认证的用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        
        // 检查用户是否有权限访问该公司
        if (currentUser.getCompanyIds() == null || 
            !currentUser.getCompanyIds().contains(switchCompanyRequest.getCompanyId())) {
            return ResponseEntity.status(403).build(); // 无权限访问该公司
        }
        
        // 更新用户当前登录公司
        currentUser.setCurrentLoginCompanyId(switchCompanyRequest.getCompanyId());
        
        // 生成新的JWT令牌
        String newToken = jwtUtil.createToken(currentUser);
        
        // 构造返回的用户信息
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(currentUser.getUserId());
        userInfo.setUsername(currentUser.getUsername());
        userInfo.setEmail(currentUser.getEmail());
        userInfo.setToken(newToken);
        userInfo.setCurrentLoginCompanyId(currentUser.getCurrentLoginCompanyId());
        
        return ResponseEntity.ok(userInfo);
    }
}