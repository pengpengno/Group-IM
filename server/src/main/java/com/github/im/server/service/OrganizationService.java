package com.github.im.server.service;

import com.github.im.server.model.Department;
import com.github.im.server.model.User;
import com.github.im.server.repository.DepartmentRepository;
import com.github.im.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrganizationService {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 获取指定公司的组织架构树
     * @param companyId 公司ID
     * @return 组织架构树
     */
    public List<Department> getOrganizationStructure(Long companyId) {
        // 获取该公司所有的有效部门
        List<Department> allDepartments = departmentRepository.findByCompanyIdAndStatusTrue(companyId);

        if (allDepartments.isEmpty()) {
            return new ArrayList<>();
        }

        // 构建ID到部门的映射
        Map<Long, Department> departmentMap = new HashMap<>();
        for (Department dept : allDepartments) {
            departmentMap.put(dept.getDepartmentId(), dept);
        }

        // 构建树形结构
        List<Department> rootDepartments = new ArrayList<>();
        for (Department dept : allDepartments) {
            Long parentId = dept.getParentId();
            if (parentId == null) {
                // 顶级部门
                rootDepartments.add(dept);
            } else {
                // 子部门
                Department parentDept = departmentMap.get(parentId);
                if (parentDept != null) {
                    if (parentDept.getChildren() == null) {
                        parentDept.setChildren(new ArrayList<>());
                    }
                    parentDept.getChildren().add(dept);
                }
            }
        }

        // 为每个部门加载成员
        loadMembersForDepartments(allDepartments, companyId);

        return rootDepartments;
    }

    /**
     * 为部门加载成员
     * @param departments 部门列表
     * @param companyId 公司ID
     */
    private void loadMembersForDepartments(List<Department> departments, Long companyId) {
        // 获取公司所有用户
        List<User> companyUsers = userRepository.findByCompanyId(companyId);

        // 按部门分组用户
        Map<Long, List<User>> usersByDepartment = companyUsers.stream()
                .filter(user -> user.getDepartmentId() != null)
                .collect(Collectors.groupingBy(User::getDepartmentId));

        // 将用户分配给对应的部门
        for (Department department : departments) {
            List<User> members = usersByDepartment.getOrDefault(department.getDepartmentId(), new ArrayList<>());
            department.setMembers(members);
        }
    }
    
    /**
     * 获取用户的组织架构（仅包含该用户所在公司）
     * @param user 用户
     * @return 组织架构树
     */
    public List<Department> getUserOrganizationStructure(User user) {
        if (user.getCompanyId() == null) {
            return new ArrayList<>();
        }
        
        return getOrganizationStructure(user.getCompanyId());
    }
}