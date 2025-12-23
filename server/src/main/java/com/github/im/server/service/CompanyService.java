package com.github.im.server.service;

import com.github.im.dto.organization.CompanyDTO;
import com.github.im.dto.organization.DepartmentDTO;
import com.github.im.dto.user.UserInfo;
import com.github.im.server.event.CompanyCreatedEvent;
import com.github.im.server.mapstruct.CompanyMapper;
import com.github.im.server.mapstruct.DepartmentMapper;
import com.github.im.server.mapstruct.UserMapper;
import com.github.im.server.model.Company;
import com.github.im.server.model.Department;
import com.github.im.server.model.User;
import com.github.im.server.model.UserDepartment;
import com.github.im.server.repository.*;
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
    
    private final CompanyUserRepository companyUserRepository;
    
    private final ApplicationEventPublisher eventPublisher;
    
    private final UserMapper userMapper;
    
    private final DepartmentMapper departmentMapper;
    
    private final CompanyMapper companyMapper;

    /**
     * 根据公司ID查找公司
     * @param companyId 公司ID
     * @return 公司对象
     */
    @Cacheable(value = "companies", key = "'company:id:' + #companyId")
    public Optional<Company> findById(Long companyId) {
        return companyRepository.findByCompanyId(companyId);
    }

    /**
     * 根据公司ID查找公司，并加载其用户信息
     * @param companyId
     * @return
     */
    @Cacheable(value = "companies", key = "'company:withUsers:' + #companyId")
    public Optional<Company> findByIdWithUsers(Long companyId) {
        return companyRepository.findByIdWithUsers(companyId);
    }

    /**
     * 根据schema名称查找公司
     * @param schemaName schema名称
     * @return 公司对象
     */
    @Cacheable(value = "companies", key = "'company:schema:' + #schemaName")
    public Optional<Company> findBySchemaName(String schemaName) {
        return companyRepository.findBySchemaName(schemaName);
    }

    /**
     * 保存公司
     * @param company 公司对象
     * @return 保存后的公司对象
     */
    @CacheEvict(value = "companies", allEntries = true)
    @Transactional  // 后面逻辑错误 则会回滚
    public Company save(Company company) {
        Company savedCompany = companyRepository.save(company);
        // 发布公司创建事件，触发schema创建
        eventPublisher.publishEvent(new CompanyCreatedEvent(savedCompany));
        
        return savedCompany;
    }

    /**
     * 注册新公司
     * @param companyDTO 公司信息DTO
     * @return 注册成功的公司DTO
     */
    public CompanyDTO registerCompany(CompanyDTO companyDTO) {
        // 转换DTO到实体
        Company company = companyMapper.companyDTOToCompany(companyDTO);
        if (company.getActive() == null) {
            company.setActive(true);
        }
        
        // 保存公司信息
        Company savedCompany = save(company);
        
        // 转换实体到DTO
        return companyMapper.companyToCompanyDTO(savedCompany);
    }
    
    /**
     * 获取所有公司列表
     * @return 公司DTO列表
     */
    public List<CompanyDTO> getAllCompanies() {
        List<Company> companies = companyRepository.findAll();
        return companyMapper.companiesToCompanyDTOs(companies);
    }

    /**
     * 获取公司的schema名称
     * @param companyId 公司ID
     * @return schema名称
     */
    public String getSchemaNameByCompanyId(Long companyId) {
        return findById(companyId).map(Company::getSchemaName).orElse("public");
    }

    /**
     * 获取公司下的所有用户
     * @param companyId 公司ID
     * @return 用户列表
     */
    public List<UserInfo> getUsersByCompanyId(Long companyId) {
        // 通过单个查询获取公司下的所有用户
        List<User> users = companyRepository.findUsersByCompanyId(companyId);
        if (users.isEmpty()) {
            return new ArrayList<>();
        }
        
        return userMapper.usersToUserInfos(users);
    }

    /**
     * 根据公司ID列表获取公司列表
     * @param companyIds 公司ID列表
     * @return 公司DTO列表
     */
    public List<CompanyDTO> getCompaniesByIds(List<Long> companyIds) {
        if (companyIds == null || companyIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Company> companies = companyRepository.findAllById(companyIds);
        return companyMapper.companiesToCompanyDTOs(companies);
    }
    
}