package com.github.im.server.controller;

import com.github.im.server.model.CompanyUser;
import com.github.im.server.service.CompanyUserService;
import com.github.im.server.web.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/company-users")
public class CompanyUserController {
    
    @Autowired
    private CompanyUserService companyUserService;
    
    /**
     * 获取用户关联的所有公司
     * @param userId 用户ID
     * @return 公司ID列表
     */
    @GetMapping("/user/{userId}/companies")
    public ApiResponse<List<Long>> getCompanyIdsByUserId(@PathVariable Long userId) {
        List<Long> companyIds = companyUserService.getCompanyIdsByUserId(userId);
        return ApiResponse.success(companyIds);
    }
    
    /**
     * 获取公司关联的所有用户
     * @param companyId 公司ID
     * @return 用户ID列表
     */
    @GetMapping("/company/{companyId}/users")
    public ApiResponse<List<Long>> getUserIdsByCompanyId(@PathVariable Long companyId) {
        List<Long> userIds = companyUserService.getUserIdsByCompanyId(companyId);
        return ApiResponse.success(userIds);
    }
    
    /**
     * 添加用户到公司
     * @param userId 用户ID
     * @param companyId 公司ID
     * @param role 用户在公司中的角色
     * @return 公司用户关联实体
     */
    @PostMapping("/user/{userId}/company/{companyId}")
    public ApiResponse<CompanyUser> addUserToCompany(
            @PathVariable Long userId,
            @PathVariable Long companyId
    ) {
        CompanyUser companyUser = companyUserService.addUserToCompany(userId, companyId);
        return ApiResponse.success(companyUser);
    }
    
    /**
     * 从公司移除用户
     * @param userId 用户ID
     * @param companyId 公司ID
     * @return 操作结果
     */
    @DeleteMapping("/user/{userId}/company/{companyId}")
    public ApiResponse<Void> removeUserFromCompany(
            @PathVariable Long userId,
            @PathVariable Long companyId) {
        companyUserService.removeUserFromCompany(userId, companyId);
        return ApiResponse.success();
    }
    
    /**
     * 更新用户在公司中的角色
     * @param userId 用户ID
     * @param companyId 公司ID
     * @param role 新角色
     * @return 更新后的公司用户关联实体
     */
    @PutMapping("/user/{userId}/company/{companyId}")
    public ApiResponse<CompanyUser> updateUserRole(
            @PathVariable Long userId,
            @PathVariable Long companyId,
            @RequestParam String role) {
        CompanyUser companyUser = companyUserService.updateUserRole(userId, companyId, role);
        return ApiResponse.success(companyUser);
    }
}