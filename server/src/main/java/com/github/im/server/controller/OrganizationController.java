package com.github.im.server.controller;

import com.github.im.server.model.Department;
import com.github.im.server.model.User;
import com.github.im.server.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organization")
public class OrganizationController {

    @Autowired
    private OrganizationService organizationService;

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
}