package com.github.im.server.controller;

import com.github.im.server.model.Company;
import com.github.im.server.model.Department;
import com.github.im.server.model.User;
import com.github.im.server.service.OrganizationService;
import com.github.im.server.service.CompanyService;
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

@RestController
@RequestMapping("/api/organization")
public class OrganizationController {

    @Autowired
    private OrganizationService organizationService;
    
    @Autowired
    private CompanyService companyService;

    /**
     * 获取组织架构
     * @param companyId 公司ID
     * @return 组织架构树
     */
    @GetMapping("/structure/{companyId}")
    public ResponseEntity<List<Department>> getOrganizationStructure(@PathVariable Long companyId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        
        // 检查用户是否有权访问该公司的组织架构
        if (!currentUser.getCompanyId().equals(companyId)) {
            return ResponseEntity.status(403).build();
        }
        
        List<Department> structure = organizationService.getOrganizationStructure(companyId);
        return ResponseEntity.ok(structure);
    }
    
    /**
     * 获取当前用户所在公司的组织架构
     * @return 组织架构树
     */
    @GetMapping("/structure")
    public ResponseEntity<List<Department>> getCurrentUserOrganizationStructure() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        List<Department> structure = organizationService.getUserOrganizationStructure(currentUser);
        return ResponseEntity.ok(structure);
    }
    
    /**
     * 注册新公司
     * @param company 公司信息
     * @return 注册结果
     */
    @PostMapping("/company/register")
    public ResponseEntity<Company> registerCompany(@RequestBody Company company) {
        // 只有系统管理员才能注册新公司
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        
        // 这里应该添加权限检查，确保只有系统管理员可以注册公司
        // 为简化起见，这里假设所有已登录用户都可以注册公司
        
        // 保存公司信息
        Company savedCompany = companyService.save(company);
        return ResponseEntity.ok(savedCompany);
    }
    
    /**
     * 获取所有公司列表
     * @return 公司列表
     */
    @GetMapping("/company")
    public ResponseEntity<List<Company>> getAllCompanies() {
        // 只有系统管理员才能查看所有公司
        List<Company> companies = companyService.findAll();
        return ResponseEntity.ok(companies);
    }
    
    /**
     * 导入部门数据
     * @param file Excel文件
     * @param companyId 公司ID
     * @return 导入结果
     */
    @PostMapping("/departments/import")
    public ResponseEntity<String> importDepartments(@RequestParam("file") MultipartFile file, 
                                                   @RequestParam("companyId") Long companyId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        
        // 检查用户是否有权导入该公司的部门数据
        if (!currentUser.getCompanyId().equals(companyId)) {
            return ResponseEntity.status(403).body("无权限导入该公司的部门数据");
        }
        
        try {
            organizationService.importDepartments(file, companyId);
            return ResponseEntity.ok("部门数据导入成功");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("导入失败: " + e.getMessage());
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
    @PostMapping("/employees/import")
    public ResponseEntity<String> importEmployees(@RequestParam("file") MultipartFile file, 
                                                 @RequestParam("companyId") Long companyId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        
        // 检查用户是否有权导入该公司的员工数据
        if (!currentUser.getCompanyId().equals(companyId)) {
            return ResponseEntity.status(403).body("无权限导入该公司的员工数据");
        }
        
        try {
            organizationService.importEmployees(file, companyId);
            return ResponseEntity.ok("员工数据导入成功");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("导入失败: " + e.getMessage());
        }
    }
    
    /**
     * 导出员工导入模板
     * @param response HTTP响应
     */
    @GetMapping("/employees/template")
    public void exportEmployeesTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=employees_template.xlsx");
        
        organizationService.generateEmployeesImportTemplate(response.getOutputStream());
    }
}