package com.github.im.server.service;

import com.github.im.dto.organization.DepartmentDTO;
import com.github.im.dto.user.UserInfo;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    
    private final DepartmentRepository departmentRepository;
    
    private final UserRepository userRepository;
    
    private final UserDepartmentRepository userDepartmentRepository;

    private final ApplicationEventPublisher eventPublisher;

    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;

    /**
     * 根据公司ID查找公司
     * @param companyId 公司ID
     * @return 公司对象
     */
    @Cacheable(value = "companies", key = "#companyId")
    public Optional<Company> findById(Long companyId) {
        return companyRepository.findById(companyId);
    }

    /**
     * 根据公司名称查找公司
     * @param name 公司名称
     * @return 公司对象
     */
    @Cacheable(value = "companiesByName", key = "#name")
    public Optional<Company> findByName(String name) {
        return companyRepository.findByName(name);
    }

    /**
     * 根据schema名称查找公司
     * @param schemaName schema名称
     * @return 公司对象
     */
    public Optional<Company> findBySchemaName(String schemaName) {
        return companyRepository.findBySchemaName(schemaName);
    }

    /**
     * 保存公司信息并创建对应的schema
     * @param company 公司对象
     * @return 保存后的公司对象
     */
    @Transactional
    @CacheEvict(value = {"companies", "companiesByName", "companiesBySchemaName"}, allEntries = true)
    public Company save(Company company) {
        // 判断 当前公司是否存在
        Optional<Company> existingCompany = companyRepository.findBySchemaName(company.getSchemaName());
        if (existingCompany.isPresent()) {
            throw new RuntimeException("Company already exists");
        }
        // 保存公司信息
        Company savedCompany = companyRepository.save(company);

        // 对于public公司，不触发事件创建schema，因为它本身就是public schema
        if (!"public".equals(company.getSchemaName())) {
            // 发布公司创建事件，触发schema创建
            eventPublisher.publishEvent(new CompanyCreatedEvent(savedCompany));
        }

        return savedCompany;
    }

    
    /**
     * 根据公司ID查找对应的schema名称
     * @param companyId 公司ID
     * @return schema名称
     */
    @Cacheable(value = "companySchemas", key = "#companyId")
    public String getSchemaNameByCompanyId(Long companyId) {
        return companyRepository.findById(companyId)
                .map(Company::getSchemaName)
                .orElse("company_" + companyId);
    }
    
    /**
     * 获取所有公司列表
     * @return 公司列表
     */
    public List<Company> findAll() {
        return companyRepository.findAll();
    }
    
    /**
     * 获取公司组织架构信息，包含部门及用户
     * 没有部门的员工将放在根节点
     * @param companyId 公司ID
     * @return 组织架构信息
     */
    public DepartmentDTO getCompanyDepartmentDto(Long companyId) {
        // 直接查询所有用户，JPA会自动加载其部门信息
        List<User> users = userRepository.findByPrimaryCompanyIdWithDepartments(companyId);
        
        // 构建用户到部门的映射
        Map<Long, Department> departmentMap = new HashMap<>();
        for (User user : users) {
            for (Department department : user.getDepartments()) {
                departmentMap.put(department.getDepartmentId(), department);
            }
        }
        
        // 转换Map的值为List
        List<Department> departments = new ArrayList<>(departmentMap.values());
        
        // 构建部门树结构
        Map<Long, Department> departmentTreeMap = new HashMap<>();
        List<Department> rootDepartments = new ArrayList<>();
        
        // 初始化所有部门到Map中
        for (Department dept : departments) {
            departmentTreeMap.put(dept.getDepartmentId(), dept);
        }
        
        // 构建父子关系
        for (Department dept : departments) {
            if (dept.getParentId() == null) {
                rootDepartments.add(dept);
            } else {
                Department parent = departmentTreeMap.get(dept.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(dept);
                }
            }
        }
        
        // 为每个部门收集用户
        Map<Long, List<User>> departmentUsersMap = new HashMap<>();
        for (User user : users) {
            for (Department department : user.getDepartments()) {
                departmentUsersMap.computeIfAbsent(department.getDepartmentId(), k -> new ArrayList<>()).add(user);
            }
        }
        
        // 为每个部门设置用户
        for (Department department : departments) {
            List<User> departmentUsers = departmentUsersMap.getOrDefault(department.getDepartmentId(), new ArrayList<>());
            department.setMembers(departmentUsers);
        }
        
        // 获取没有分配部门的用户
        List<User> unassignedUsers = users.stream()
                .filter(user -> user.getDepartments().isEmpty())
                .collect(Collectors.toList());
        
        // 使用Mapper转换为DTO
        List<DepartmentDTO> departmentDTOs = departmentMapper.departmentsToDepartmentDTOs(rootDepartments);
        DepartmentDTO root = new DepartmentDTO();
        root.setCompanyId(companyId);
        root.setMembers(userMapper.usersToUserInfos(unassignedUsers));
        root.setChildren(departmentDTOs);
        // 创建一个特殊的"未分配"部门来存放没有部门的用户

        return root;
    }
    
    /**
     * 获取没有分配到任何部门的用户
     * @param companyId 公司ID
     * @param departments 部门列表
     * @return 未分配用户列表
     */
    private List<User> getUnassignedUsers(Long companyId, List<Department> departments) {
        // 获取所有部门ID
        List<Long> departmentIds = departments.stream()
                .map(Department::getDepartmentId)
                .collect(Collectors.toList());
        
        // 获取所有部门的用户关联信息
        List<UserDepartment> userDepartments = userDepartmentRepository.findByDepartmentIdIn(departmentIds);
        
        // 获取所有用户信息
        List<User> allCompanyUsers = userRepository.findByPrimaryCompanyId(companyId);
        
        // 获取没有分配部门的用户
        List<Long> assignedUserIds = userDepartments.stream()
                .map(UserDepartment::getUserId)
                .collect(Collectors.toList());
        
        return allCompanyUsers.stream()
                .filter(user -> !assignedUserIds.contains(user.getUserId()))
                .collect(Collectors.toList());
    }

}