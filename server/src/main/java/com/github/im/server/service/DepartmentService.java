package com.github.im.server.service;

import com.github.im.dto.organization.DepartmentDTO;
import com.github.im.server.event.CompanyCreatedEvent;
import com.github.im.server.mapstruct.DepartmentMapper;
import com.github.im.server.mapstruct.UserMapper;
import com.github.im.server.model.Company;
import com.github.im.server.model.Department;
import com.github.im.server.model.User;
import com.github.im.server.model.UserDepartment;
import com.github.im.server.repository.CompanyRepository;
import com.github.im.server.repository.DepartmentRepository;
import com.github.im.server.repository.UserDepartmentRepository;
import com.github.im.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final CompanyRepository companyRepository;

    private final DepartmentRepository departmentRepository;

    private final UserRepository userRepository;

    private final UserDepartmentRepository userDepartmentRepository;

    private final ApplicationEventPublisher eventPublisher;

    private final UserMapper userMapper;

    private final DepartmentMapper departmentMapper;

    /**
     * 获取公司组织架构信息，包含部门及用户
     * 没有部门的员工将放在根节点
     * @return 组织架构信息
     */
    public List<DepartmentDTO> getCompanyDepartmentDto(Long companyId) {
        // 获取公司下所有启用的部门
        List<Department> allDepartments = departmentRepository.findAll();
        
        // 获取所有部门ID
        List<Long> departmentIds = allDepartments.stream()
                .map(Department::getDepartmentId)
                .collect(Collectors.toList());
        
        // 获取所有部门的用户关联信息
        List<UserDepartment> userDepartments = userDepartmentRepository.findByDepartmentIdIn(departmentIds);
        
        // 获取所有用户信息
        List<User> allCompanyUsers = userRepository.findAll();
        Map<Long, User> userMap = allCompanyUsers.stream()
                .collect(Collectors.toMap(User::getUserId, user -> user));
        
        // 将用户分配到对应的部门
        Map<Long, List<User>> departmentUsersMap = userDepartments.stream()
                .collect(Collectors.groupingBy(UserDepartment::getDepartmentId))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(ud -> userMap.get(ud.getUserId()))
                                .filter(java.util.Objects::nonNull)
                                .collect(Collectors.toList())
                ));
        
        // 为每个部门设置用户
        allDepartments.forEach(department -> {
            List<User> departmentUsers = departmentUsersMap.getOrDefault(department.getDepartmentId(), new ArrayList<>());
            department.setMembers(departmentUsers);
        });
        
        // 获取没有分配部门的用户
        List<Long> assignedUserIds = userDepartments.stream()
                .map(UserDepartment::getUserId)
                .collect(Collectors.toList());
        
        List<User> unassignedUsers = allCompanyUsers.stream()
                .filter(user -> !assignedUserIds.contains(user.getUserId()))
                .collect(Collectors.toList());
        
        // 转换为DTO
        List<DepartmentDTO> departmentDTOs = allDepartments.stream()
                .map(this::convertToDepartmentDTO)
                .collect(Collectors.toList());
        
        // 创建一个特殊的"未分配"部门来存放没有部门的用户
        if (!unassignedUsers.isEmpty()) {
            DepartmentDTO unassignedDept = new DepartmentDTO();
            unassignedDept.setDepartmentId(-1L);
            unassignedDept.setName("未分配员工");
            unassignedDept.setDescription("没有分配到任何部门的员工");
            unassignedDept.setCompanyId(companyId);
            unassignedDept.setMembers(unassignedUsers.stream()
                    .map(this::convertUserToUserInfo)
                    .collect(Collectors.toList()));
            
            departmentDTOs.add(unassignedDept);
        }
        
        return departmentDTOs;
    }
    
    /**
     * 将Department转换为DepartmentDTO
     * @param department 部门实体
     * @return 部门DTO
     */
    private DepartmentDTO convertToDepartmentDTO(Department department) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setDepartmentId(department.getDepartmentId());
        dto.setName(department.getName());
        dto.setDescription(department.getDescription());
        dto.setCompanyId(department.getCompanyId());
        dto.setParentId(department.getParentId());
        dto.setOrderNum(department.getOrderNum());
        dto.setStatus(department.getStatus());
        dto.setCreatedAt(department.getCreatedAt());
        dto.setUpdatedAt(department.getUpdatedAt());
        
        // 转换子部门
        if (department.getChildren() != null) {
            dto.setChildren(department.getChildren().stream()
                    .map(this::convertToDepartmentDTO)
                    .collect(Collectors.toList()));
        }
        
        // 转换成员
        if (department.getMembers() != null) {
            dto.setMembers(department.getMembers().stream()
                    .map(this::convertUserToUserInfo)
                    .collect(Collectors.toList()));
        }
        
        return dto;
    }
    
    /**
     * 将User转换为UserInfo
     * @param user 用户实体
     * @return 用户信息DTO
     */
    private com.github.im.dto.user.UserInfo convertUserToUserInfo(User user) {
        com.github.im.dto.user.UserInfo userInfo = new com.github.im.dto.user.UserInfo();
        userInfo.setUserId(user.getUserId());
        userInfo.setUsername(user.getUsername());
        userInfo.setEmail(user.getEmail());
        userInfo.setPhoneNumber(user.getPhoneNumber());
        return userInfo;
    }
}