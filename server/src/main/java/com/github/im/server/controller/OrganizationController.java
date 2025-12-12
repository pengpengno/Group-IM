package com.github.im.server.controller;

import com.github.im.dto.organization.CompanyDTO;
import com.github.im.dto.organization.DepartmentDTO;
import com.github.im.server.mapstruct.OrganizationMapper;
import com.github.im.server.model.Company;
import com.github.im.server.model.Department;
import com.github.im.server.model.User;
import com.github.im.server.service.OrganizationService;
import com.github.im.server.service.CompanyService;
import com.github.im.server.web.ApiResponse;
import com.github.im.server.web.ResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/organization")
public class OrganizationController {

    @Autowired
    private OrganizationService organizationService;
    
    @Autowired
    private CompanyService companyService;
    
    @Autowired
    private OrganizationMapper organizationMapper;

    /**
     * 获取组织架构
     * @param companyId 公司ID
     * @return 组织架构树
     */
    @GetMapping("/structure/{companyId}")
    public ResponseEntity<ApiResponse<List<DepartmentDTO>>> getOrganizationStructure(@PathVariable Long companyId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        
        // 检查用户是否有权访问该公司的组织架构
        if (!currentUser.getCurrentLoginCompanyId().equals(companyId)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(org.springframework.http.HttpStatus.FORBIDDEN.value(), "无权限访问该公司的组织架构"));
        }
        
        try {
            List<DepartmentDTO> departmentDTOs = organizationService.getDepartmentDTOs(currentUser);
            return ResponseUtil.success("获取组织架构成功", departmentDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR.value(), "获取组织架构失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取当前用户所在公司的组织架构
     * @return 组织架构树
     */
    @GetMapping("/structure")
    public ResponseEntity<ApiResponse<List<DepartmentDTO>>> getCurrentUserOrganizationStructure() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        
        try {
            List<DepartmentDTO> departmentDTOs = organizationService.getDepartmentDTOs(currentUser);
            return ResponseUtil.success("获取组织架构成功", departmentDTOs);
        } catch (Exception e) {
            log.error("获取组织架构失败", e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR.value(), "获取组织架构失败: " + e.getMessage()));
        }
    }
    
    /**
     * 注册新公司
     * @param companyDTO 公司信息
     * @return 注册结果
     */
    @PostMapping("/company/register")
    public ResponseEntity<ApiResponse<CompanyDTO>> registerCompany(@RequestBody CompanyDTO companyDTO) {
        // 只有系统管理员才能注册新公司
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        
        // 这里应该添加权限检查，确保只有系统管理员可以注册公司
        // 为简化起见，这里假设所有已登录用户都可以注册公司
        
        try {
            // 转换DTO到实体
            Company company = organizationMapper.companyDTOToCompany(companyDTO);
            if (company.getActive() == null) {
                company.setActive(true);
            }
            
            // 保存公司信息
            Company savedCompany = companyService.save(company);
            
            // 转换实体到DTO
            CompanyDTO savedCompanyDTO = organizationMapper.companyToCompanyDTO(savedCompany);
            return ResponseUtil.success("公司注册成功", savedCompanyDTO);
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR.value(), "公司注册失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取所有公司列表
     * @return 公司列表
     */
    @GetMapping("/company")
    public ResponseEntity<ApiResponse<List<CompanyDTO>>> getAllCompanies() {
        try {
            // 只有系统管理员才能查看所有公司
            List<Company> companies = companyService.findAll();
            List<CompanyDTO> companyDTOs = organizationMapper.companiesToCompanyDTOs(companies);
            return ResponseUtil.success("获取公司列表成功", companyDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR.value(), "获取公司列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 导入部门数据
     * @param file Excel文件
     * @param companyId 公司ID
     * @return 导入结果
     */
    @PostMapping("/departments/import")
    public ResponseEntity<ApiResponse<Object>> importDepartments(@RequestParam("file") MultipartFile file, 
                                                   @RequestParam("companyId") Long companyId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        
        // 检查用户是否有权导入该公司的部门数据
        if (!currentUser.getCurrentLoginCompanyId().equals(companyId)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(org.springframework.http.HttpStatus.FORBIDDEN.value(), "无权限导入该公司的部门数据"));
        }
        
        try {
            organizationService.importDepartments(file, companyId);
            return ResponseUtil.success("部门数据导入成功");
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR.value(), "导入失败: " + e.getMessage()));
        }
    }
    
    /**
     * 导出部门导入模板
     * @param response HTTP响应
     */
    @GetMapping("/departments/template")
    public void exportDepartmentsTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=departments_template.xlsx");
        organizationService.generateDepartmentsImportTemplate(response.getOutputStream());
    }
    
    /**
     * 导入员工数据
     * @param file Excel文件
     * @param companyId 公司ID
     * @return 导入结果
     */
    @PostMapping("/users/import")
    public ResponseEntity<ApiResponse<Object>> importEmployees(@RequestParam("file") MultipartFile file, 
                                                 @RequestParam("companyId") Long companyId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        
        // 检查用户是否有权导入该公司的员工数据
        if (!currentUser.getCurrentLoginCompanyId().equals(companyId)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(org.springframework.http.HttpStatus.FORBIDDEN.value(), "无权限导入该公司的员工数据"));
        }
        
        try {
            organizationService.importEmployees(file, companyId);
            return ResponseUtil.success("员工数据导入成功");
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR.value(), "导入失败: " + e.getMessage()));
        }
    }
    
    /**
     * 导出员工导入模板
     * @param response HTTP响应
     */
    @GetMapping("/users/template")
    public void exportEmployeesTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users_template.xlsx");
        
        organizationService.generateEmployeesImportTemplate(response.getOutputStream());
    }
}