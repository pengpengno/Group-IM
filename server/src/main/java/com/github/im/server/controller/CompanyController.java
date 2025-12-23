package com.github.im.server.controller;

import com.github.im.dto.organization.DepartmentDTO;
import com.github.im.dto.user.BatchUserDepartmentRequest;
import com.github.im.server.model.User;
import com.github.im.server.service.*;
import com.github.im.server.web.ApiResponse;
import com.github.im.server.web.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final CompanyUserService companyUserService;

    /**
     * 获取当前用户所在公司的组织架构
     * @return 组织架构树
     */
    @GetMapping("/structure")
    public ResponseEntity<ApiResponse<List<DepartmentDTO>>> getCurrentUserOrganizationStructure(
            @AuthenticationPrincipal User user
    ) {

        try {
            List<DepartmentDTO> departmentDTOs = organizationService.getDepartmentDTOs(user);
            return ResponseUtil.success("获取组织架构成功", departmentDTOs);
        } catch (Exception e) {
            log.error("获取组织架构失败", e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR.value(), "获取组织架构失败: " + e.getMessage()));
        }
    }

    /**
     * 创建部门
     * @param departmentDTO 部门信息
     * @return 创建结果
     */
    @PostMapping("/department")
    public ResponseEntity<ApiResponse<DepartmentDTO>> createDepartment(@RequestBody DepartmentDTO departmentDTO, @AuthenticationPrincipal User user) {
        try {
            if (user == null) {
                return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "用户未认证"));
            }

            Long companyId = user.getCurrentCompany().getCompanyId();

            if (companyId == null) {
                return ResponseEntity.status(400)
                    .body(ApiResponse.error(400, "用户未选择公司"));
            }

            // 设置公司ID
            departmentDTO.setCompanyId(companyId);
            
            // 创建部门
            DepartmentDTO result = organizationService.createDepartment(departmentDTO);

            return ResponseUtil.success("部门创建成功", result);
        } catch (Exception e) {
            log.error("创建部门失败", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error(500, "创建部门失败: " + e.getMessage()));
        }
    }

    /**
     * 更新部门信息
     * @param departmentId 部门ID
     * @param departmentDTO 部门信息
     * @return 更新结果
     */
    @PutMapping("/department/{departmentId}")
    public ResponseEntity<ApiResponse<DepartmentDTO>> updateDepartment(
            @PathVariable Long departmentId, 
            @RequestBody DepartmentDTO departmentDTO, @AuthenticationPrincipal User user) {
        try {
            if (user == null) {
                return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "用户未认证"));
            }

            Long companyId = user.getCurrentCompany().getCompanyId();

            if (companyId == null) {
                return ResponseEntity.status(400)
                    .body(ApiResponse.error(400, "用户未选择公司"));
            }

            // 更新部门
            DepartmentDTO result = organizationService.updateDepartment(departmentId, departmentDTO);

            return ResponseUtil.success("部门更新成功", result);
        } catch (Exception e) {
            log.error("更新部门失败", e);
            return ResponseEntity.status(500)
                  .body(ApiResponse.error(500, "更新部门失败: " + e.getMessage()));
        }
    }

    /**
     * 删除部门
     * @param departmentId 部门ID
     * @return 删除结果
     */
    @DeleteMapping("/department/{departmentId}")
    public ResponseEntity<ApiResponse<Boolean>> deleteDepartment(@PathVariable Long departmentId, @AuthenticationPrincipal User user) {
        try {
            // 删除部门
            organizationService.deleteDepartment(departmentId);

            return ResponseUtil.success("部门删除成功", true);
        } catch (Exception e) {
            log.error("删除部门失败", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error(500, "删除部门失败: " + e.getMessage()));
        }
    }

    /**
     * 移动部门到新的父部门下
     * @param departmentId 部门ID
     * @param newParentId 新的父部门ID，如果为null则移动到根节点
     * @return 移动结果
     */
    @PatchMapping("/department/{departmentId}/move")
    public ResponseEntity<ApiResponse<DepartmentDTO>> moveDepartment(
            @PathVariable Long departmentId, 
            @RequestParam(required = false) Long newParentId, @AuthenticationPrincipal User user) {
        try {
            if (user == null) {
                return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "用户未认证"));
            }

            Long companyId = user.getCurrentCompany().getCompanyId();

            if (companyId == null) {
                return ResponseEntity.status(400)
                    .body(ApiResponse.error(400, "用户未选择公司"));
            }

            // 移动部门
            DepartmentDTO result = organizationService.moveDepartment(departmentId, newParentId);

            return ResponseUtil.success("部门移动成功", result);
        } catch (Exception e) {
            log.error("移动部门失败", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error(500, "移动部门失败: " + e.getMessage()));
        }
    }

    /**
     * 将用户分配到部门
     * @param userId 用户ID
     * @param departmentId 部门ID
     * @return 分配结果
     */
    @PostMapping("/department/{departmentId}/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Boolean>> assignUserToDepartment(
            @PathVariable Long userId, 
            @PathVariable Long departmentId, @AuthenticationPrincipal User user) {
        try {


            // 分配用户到部门
            organizationService.assignUserToDepartment(userId, departmentId);

            return ResponseUtil.success("用户分配到部门成功", true);
        } catch (Exception e) {
            log.error("用户分配到部门失败", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error(500, "用户分配到部门失败: " + e.getMessage()));
        }
    }

    /**
     * 批量将用户分配到部门
     * @param request 包含用户ID列表的请求对象
     * @param departmentId 部门ID
     * @return 分配结果
     */
    @PostMapping("/department/{departmentId}/users")
    public ResponseEntity<ApiResponse<Boolean>> batchAssignUsersToDepartment(
            @Valid @RequestBody BatchUserDepartmentRequest request,
            @PathVariable Long departmentId, @AuthenticationPrincipal User user) {
        try {

            // 批量分配用户到部门
            organizationService.batchAssignUsersToDepartment(request.getUserIds(), departmentId);

            return ResponseUtil.success("批量用户分配到部门成功", true);
        } catch (Exception e) {
            log.error("批量用户分配到部门失败", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error(500, "批量用户分配到部门失败: " + e.getMessage()));
        }
    }

    /**
     * 将用户从部门移除
     * @param userId 用户ID
     * @param departmentId 部门ID
     * @return 移除结果
     */
    @DeleteMapping("/department/{departmentId}/user/{userId}")
    public ResponseEntity<ApiResponse<Boolean>> removeUserFromDepartment(
            @PathVariable Long userId, 
            @PathVariable Long departmentId, @AuthenticationPrincipal User user) {
        try {
            if (user == null) {
                return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "用户未认证"));
            }

            Long companyId = user.getCurrentCompany().getCompanyId();

            if (companyId == null) {
                return ResponseEntity.status(400)
                    .body(ApiResponse.error(400, "用户未选择公司"));
            }

            // 从部门移除用户
            organizationService.removeUserFromDepartment(userId, departmentId);

            return ResponseUtil.success("从部门移除用户成功", true);
        } catch (Exception e) {
            log.error("从部门移除用户失败", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error(500, "从部门移除用户失败: " + e.getMessage()));
        }
    }

    /**
     * 调整公司组织架构
     * @param departmentDTOS 组织架构信息
     * @return 调整结果
     */
    @PutMapping("/organization")
    public ResponseEntity<ApiResponse<Boolean>> adjustCompanyOrganization(@RequestBody List<DepartmentDTO> departmentDTOS, @AuthenticationPrincipal User user) {
        try {
            if (user == null) {
                return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "用户未认证"));
            }

            Long companyId = user.getCurrentCompany().getCompanyId();

            if (companyId == null) {
                return ResponseEntity.status(400)
                    .body(ApiResponse.error(400, "用户未选择公司"));
            }

            // 调整组织架构
            organizationService.adjustOrganizationStructure(companyId, departmentDTOS);

            return ResponseUtil.success("组织架构调整成功", true);
        } catch (Exception e) {
            log.error("调整公司组织架构失败", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error(500, "调整公司组织架构失败: " + e.getMessage()));
        }
    }

    /**
     * 获取当前用户所在公司的组织架构信息，包含部门及用户
     * 没有部门的员工将放在根节点
     * @return 组织架构信息
     */
    @GetMapping("/departmentInfo")
    public ResponseEntity<ApiResponse<DepartmentDTO>> getCurrentCompanyOrganization(@AuthenticationPrincipal User user) {
        try {
            if (user == null) {
                return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "用户未认证"));
            }
            
            Long companyId = user.getCurrentCompany().getCompanyId();
            
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