package com.github.im.server.controller;

import com.github.im.dto.organization.DepartmentDTO;
import com.github.im.dto.user.UserInfo;
import com.github.im.server.model.User;
import com.github.im.server.service.CompanyService;
import com.github.im.server.service.DepartmentService;
import com.github.im.server.service.OrganizationService;
import com.github.im.server.service.UserService;
import com.github.im.server.web.ApiResponse;
import com.github.im.server.web.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
public class CompanyController {
    
    private final CompanyService companyService;
    private final OrganizationService organizationService;
    private final UserService userService;
    private final DepartmentService departmentService;

    /**
     * 获取当前用户所在公司的组织架构信息，包含部门及用户
     * 没有部门的员工将放在根节点
     * @return 组织架构信息
     */
    @GetMapping("/departmentInfo")
    public ResponseEntity<ApiResponse<DepartmentDTO>> getCurrentCompanyOrganization() {
        try {
            // 从SecurityContext获取当前认证用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "用户未认证"));
            }
            
            // 获取当前用户信息
            Object principal = authentication.getPrincipal();
            if (!(principal instanceof User)) {
                return ResponseEntity.status(400)
                    .body(ApiResponse.error(400, "用户信息无效"));
            }
            
            User currentUser = (User) principal;
            Long companyId = currentUser.getCurrentCompany().getCompanyId();
            
            if (companyId == null) {
                return ResponseEntity.status(400)
                    .body(ApiResponse.error(400, "用户未选择公司"));
            }

            // 获取公司组织架构
            var  departmentDTO = departmentService.getCompanyDepartmentDto(companyId);

            return ResponseUtil.success("获取公司组织架构成功", departmentDTO);
        } catch (Exception e) {
            log.error("获取公司组织架构失败", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error(500, "获取公司组织架构失败: " + e.getMessage()));
        }
    }
}