package com.github.im.server.controller;

import com.github.im.dto.PageResult;
import com.github.im.dto.organization.CompanyDTO;
import com.github.im.dto.user.LoginRequest;
import com.github.im.dto.user.RegistrationRequest;
import com.github.im.dto.user.UserInfo;
import com.github.im.server.model.User;
import com.github.im.server.service.CompanyUserService;
import com.github.im.server.service.UserService;
import com.github.im.server.web.ApiResponse;
import com.github.im.server.web.ResponseUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/users")
@Valid
public class UserController {
    @Autowired
    private UserService userService;
    
    @Autowired
    private CompanyUserService companyUserService;

    @PostMapping("/register")
    public ResponseEntity<UserInfo> registerUser(@RequestBody @Valid RegistrationRequest registrationRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.registerUser(registrationRequest).get());
    }

    /**
     * 获取当前用户所属的公司列表
     * @return 当前用户所属的公司列表
     */
    @GetMapping("/company/list")
    @PreAuthorize("isAuthenticated()")
//    @PreAuthorize("@companyOwnershipChecker.hasCompanyAccess(#user, #departmentDTO.companyId)")
    public ResponseEntity<ApiResponse<List<CompanyDTO>>> getMyCompanies(@AuthenticationPrincipal User user) {
        try {

            // 查询当前用户所属的公司列表
            List<CompanyDTO> companies = companyUserService.getCompanyByUserId(user.getUserId());

            return ResponseUtil.success("获取公司列表成功", companies);
        } catch (Exception e) {
            log.error("获取用户公司列表失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "获取用户公司列表失败: " + e.getMessage()));
        }
    }

    @GetMapping("/{username}")
    public Optional<User> getUserByUsername(@PathVariable String username) {
        //TODO 权限
        return userService.findUserByUsername(username);
    }


    /**
     * 根据传入的查询字符串查询用户
     * @param query  查询条件用户名 获取  Email
     * @return 与之对应的信息
     */
    @PostMapping("/query")
    public ResponseEntity<PagedModel<UserInfo>> queryUserByNameOrEmail(@RequestParam String query ,
                                                                       @RequestHeader("Authorization") String auth) {
        log.debug("query: " + query);
        return ResponseEntity.ok(new PagedModel<>(userService.findUserByQueryStrings(query, SecurityContextHolder.getContext().getAuthentication())));
    }


    // 用户登录
    @PostMapping("/login")
    public ResponseEntity<UserInfo> loginUser(@RequestBody @NotNull(message = "login request  must not be null") LoginRequest loginRequest) {
        Optional<UserInfo> userInfo = userService.loginUser(loginRequest);
        // 登录失败
        return userInfo.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }


    // 重置用户密码
    @PutMapping("/reset-password/{userId}")
    public User resetPassword(@PathVariable Long userId, @RequestBody String newPassword) {
        return userService.resetPassword(userId, newPassword);
    }


}