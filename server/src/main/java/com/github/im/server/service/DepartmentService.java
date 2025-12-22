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
     * 公司作为顶层节点，部门作为子节点，没有部门的员工将放在公司节点下
     * @return 组织架构信息
     */
    public DepartmentDTO getCompanyDepartmentDto(Long companyId) {
        // 获取公司信息
        Optional<Company> companyOpt = companyRepository.findById(companyId);
        if (companyOpt.isEmpty()) {
            return new DepartmentDTO();
        }
        
        Company company = companyOpt.get();
        
        // 获取公司下所有启用的部门
        List<Department> allDepartments = departmentRepository.findByCompanyIdAndStatusTrue(companyId);
        
        // 构建部门树结构
        Map<Long, Department> departmentMap = allDepartments.stream()
                .collect(Collectors.toMap(Department::getDepartmentId, d -> d));
        
        List<Department> rootDepartments = new ArrayList<>();
        
        // 构建父子关系
        for (Department dept : allDepartments) {
            if (dept.getParentId() == null) {
                rootDepartments.add(dept);
            } else {
                Department parent = departmentMap.get(dept.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(dept);
                }
            }
        }
        
        // 获取所有部门ID
        List<Long> departmentIds = allDepartments.stream()
                .map(Department::getDepartmentId)
                .collect(Collectors.toList());
        
        // 获取所有部门的用户关联信息
        List<UserDepartment> userDepartments = new ArrayList<>();
        if (!departmentIds.isEmpty()) {
            userDepartments = userDepartmentRepository.findByDepartmentIdIn(departmentIds);
        }
        
        // 获取所有用户信息
        List<User> allCompanyUsers = userRepository.findByPrimaryCompanyId(companyId);
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
        List<DepartmentDTO> rootDepartmentDTOs = departmentMapper.departmentsToDepartmentDTOs(rootDepartments);
        
        // 创建公司节点作为顶层节点
        DepartmentDTO companyDto = new DepartmentDTO();
        companyDto.setDepartmentId(company.getCompanyId());
        companyDto.setName(company.getName());
        companyDto.setDescription("公司顶层节点");
        companyDto.setCompanyId(companyId);
        companyDto.setChildren(rootDepartmentDTOs);
        
        // 将未分配的用户放到公司节点下
        if (!unassignedUsers.isEmpty()) {
            companyDto.setMembers(userMapper.usersToUserInfos(unassignedUsers));
        }
        
        // 返回只包含公司节点的列表，公司节点下包含所有部门
        List<DepartmentDTO> result = new ArrayList<>();
        result.add(companyDto);
        
        return companyDto;
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