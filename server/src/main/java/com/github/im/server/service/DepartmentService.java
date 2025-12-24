package com.github.im.server.service;

import com.github.im.dto.organization.DepartmentDTO;
import com.github.im.dto.user.UserBasicInfo;
import com.github.im.dto.user.UserInfo;
import com.github.im.server.event.CompanyCreatedEvent;
import com.github.im.server.exception.BusinessException;
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
import com.github.im.server.util.SchemaSwitcher;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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
    
    private final CompanyService companyService;
    private final CompanyUserService companyUserService;

    private final EntityManager entityManager;

    /**
     * 将用户分配到部门（移动用户到新部门）
     * @param userId 用户ID
     * @param departmentId 部门ID
     */
    @Transactional
    public void assignUserToDepartment(Long userId, Long departmentId) {
        // 先找到部门所属的公司
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("部门不存在"));

        // 验证用户是否属于当前公司
        if (!companyUserService.isUserInCompany(userId, department.getCompanyId())) {
            throw new RuntimeException("用户不属于当前公司");
        }

        // 先删除用户在当前部门的关联关系
        userDepartmentRepository.deleteByUserId(userId);
        userDepartmentRepository.flush(); // 删除了避免 HIB 中对于sql的执行顺序  ，先删除

        userDepartmentRepository.save(new UserDepartment(userId,departmentId));

    }

    /**
     * 批量将用户分配到部门（移动用户到新部门）
     * @param userIds 用户ID列表
     * @param departmentId 部门ID
     */
    @Transactional
    public void batchAssignUsersToDepartment(List<Long> userIds, Long departmentId) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        // 先找到部门所属的公司
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new BusinessException("部门不存在"));


        // 验证所有用户是否存在于当前公司中
        List<Long> validUserIds = companyUserService.getValidUserIdsInCompany(userIds, department.getCompanyId());

        if (validUserIds.size() != userIds.size()) {
            // 检查哪些用户无效
            List<Long> invalidUserIds = new ArrayList<>(userIds);
            invalidUserIds.removeAll(validUserIds);
            throw new BusinessException("以下用户不属于当前公司: " + invalidUserIds);
        }
        userDepartmentRepository.deleteByUserIdIn(userIds);
        userDepartmentRepository.flush(); // 删除了避免 HIB 中对于sql的执行顺序  ，先删除

        List<UserDepartment> userDepartments = userIds.stream().map(uId -> {
            return new UserDepartment(uId, departmentId);
        }).toList();
        userDepartmentRepository.saveAll(userDepartments);

    }

    /**
     * 将用户从部门移除
     * @param userId 用户ID
     * @param departmentId 部门ID
     */
    @Transactional
    public void removeUserFromDepartment(Long userId, Long departmentId) {
        // 先找到部门所属的公司
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("部门不存在"));

        userDepartmentRepository.deleteByUserIdAndDepartmentId(userId, departmentId);
    }


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
            department.setMembers(userMapper.usersToUserBasicInfos(departmentUsers));
        });
        
        // 获取没有分配部门的用户
        List<Long> assignedUserIds = userDepartments.stream()
                .map(UserDepartment::getUserId)
                .toList();
        
        List<User> unassignedUsers = allCompanyUsers.stream()
                .filter(user -> !assignedUserIds.contains(user.getUserId()))
                .collect(Collectors.toList());

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
            companyDto.setMembers(userMapper.usersToUserBasicInfos(unassignedUsers));
        }
        
        // 返回只包含公司节点的列表，公司节点下包含所有部门
        List<DepartmentDTO> result = new ArrayList<>();
        result.add(companyDto);
        
        return companyDto;
    }
    
    /**
     * 创建部门
     * @param departmentDTO 部门信息
     * @return 创建后的部门信息
     */
    @Transactional
    public DepartmentDTO createDepartment(DepartmentDTO departmentDTO) {
        String schemaName = companyService.getSchemaNameByCompanyId(departmentDTO.getCompanyId());
        
        return SchemaSwitcher.executeWithFreshConnectionInSchema(entityManager, schemaName, em -> {
            Department department = new Department();
            department.setName(departmentDTO.getName());
            department.setDescription(departmentDTO.getDescription());
            department.setCompanyId(departmentDTO.getCompanyId());
            department.setParentId(departmentDTO.getParentId());
            department.setOrderNum(departmentDTO.getOrderNum());
            department.setStatus(departmentDTO.getStatus() != null ? departmentDTO.getStatus() : true);

            Department savedDepartment = departmentRepository.save(department);
            return departmentMapper.departmentToDepartmentDTO(savedDepartment);
        });
    }

    /**
     * 更新部门信息
     * @param departmentId 部门ID
     * @param departmentDTO 部门信息
     * @return 更新后的部门信息
     */
    @Transactional
    public DepartmentDTO updateDepartment(Long departmentId, DepartmentDTO departmentDTO) {
        // 先找到部门所属的公司
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("部门不存在"));
        
        String schemaName = companyService.getSchemaNameByCompanyId(department.getCompanyId());
        
        return SchemaSwitcher.executeWithFreshConnectionInSchema(entityManager, schemaName, em -> {
            department.setName(departmentDTO.getName());
            department.setDescription(departmentDTO.getDescription());
            if (departmentDTO.getOrderNum() != null) {
                department.setOrderNum(departmentDTO.getOrderNum());
            }
            if (departmentDTO.getStatus() != null) {
                department.setStatus(departmentDTO.getStatus());
            }
            
            Department savedDepartment = departmentRepository.save(department);
            return departmentMapper.departmentToDepartmentDTO(savedDepartment);
        });
    }

    /**
     * 删除部门
     * @param departmentId 部门ID
     */
    @Transactional
    public void deleteDepartment(Long departmentId) {
        // 先找到 部门 所属的 公司
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("部门不存在!"));

        var count = userDepartmentRepository.deleteByDepartmentId(departmentId);

        departmentRepository.deleteById(departmentId);
    }

    /**
     * 移动部门到新的父部门下
     * @param departmentId 部门ID
     * @param newParentId 新的父部门ID，如果为null则移动到根节点
     * @return 移动后的部门信息
     */
    @Transactional
    public DepartmentDTO moveDepartment(Long departmentId, Long newParentId) {
        // 先找到部门所属的公司
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("部门不存在"));
        department.setParentId(newParentId);
        Department savedDepartment = departmentRepository.save(department);
        return departmentMapper.departmentToDepartmentDTO(savedDepartment);
    }
    
    /**
     * 获取组织架构树
     * @param companyId 公司ID
     * @return 组织架构树
     */
    @Cacheable(value = "organizationStructure", key = "#companyId")
    public List<Department> getOrganizationStructure(Long companyId) {
        // 获取公司下所有启用的部门
        List<Department> allDepartments = departmentRepository.findByCompanyIdAndStatusTrue(companyId);

        // 构建部门树结构
        Map<Long, Department> departmentMap = new HashMap<>();
        List<Department> rootDepartments = new ArrayList<>();
        
        // 初始化所有部门到Map中
        for (Department dept : allDepartments) {
            departmentMap.put(dept.getDepartmentId(), dept);
        }
        
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
        
        return rootDepartments;
    }
    
    /**
     * 获取用户当前登录公司的信息
     * @param user
     * @return 返回部门Dto
     */
    public List<DepartmentDTO> getDepartmentDTOs(User user) {
        return departmentMapper.departmentsToDepartmentDTOs(getOrganizationStructure(user.getCurrentCompany().getCompanyId()));
    }

    /**
     * 获取指定公司的部门信息
     * @return 返回部门信息
     */
    public List<DepartmentDTO> getDepartmentDTOs(Long companyUserId) {
        return
            departmentMapper.departmentsToDepartmentDTOs(getOrganizationStructure(companyUserId));

    }
    
    /**
     * 获取用户的组织架构（仅包含该用户所在公司）
     * @param user 用户
     * @return 组织架构树
     */
    public List<Department> getUserOrganizationStructure(User user) {
        if (user.getCurrentCompany() == null) {
            return new ArrayList<>();
        }
        
        return getOrganizationStructure(user.getCurrentCompany().getCompanyId());
    }

    /**
     * 将User转换为UserInfo
     * @param user 用户实体
     * @return 用户信息DTO
     */
    private UserInfo convertUserToUserInfo(User user) {
        UserInfo userInfo = new com.github.im.dto.user.UserInfo();
        userInfo.setUserId(user.getUserId());
        userInfo.setUsername(user.getUsername());
        userInfo.setEmail(user.getEmail());
        userInfo.setPhoneNumber(user.getPhoneNumber());
        return userInfo;
    }
}