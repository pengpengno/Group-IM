package com.github.im.server.service;

import com.github.im.dto.organization.CompanyDTO;
import com.github.im.dto.organization.DepartmentDTO;
import com.github.im.dto.user.UserInfo;
import com.github.im.server.event.CompanyCreatedEvent;
import com.github.im.server.exception.BusinessException;
import com.github.im.server.mapstruct.CompanyMapper;
import com.github.im.server.mapstruct.DepartmentMapper;
import com.github.im.server.mapstruct.UserMapper;
import com.github.im.server.model.Company;
import com.github.im.server.model.Department;
import com.github.im.server.model.User;
import com.github.im.server.model.UserDepartment;
import com.github.im.server.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Validated
@Slf4j
public class CompanyService {

    private final CompanyRepository companyRepository;

    private final ApplicationEventPublisher eventPublisher;
    
    private final UserMapper userMapper;

    private final CompanyMapper companyMapper;
    private final EntityManager entityManager;


    // Schema name validation regex - only allow alphanumeric characters and underscores
    private static final String SCHEMA_NAME_PATTERN = "^[a-zA-Z0-9_]+$";
    /**
     * 根据公司ID查找公司
     * @param companyId 公司ID
     * @return 公司对象
     */
    public Optional<Company> findById(Long companyId) {
        return companyRepository.findByCompanyId(companyId);
    }

    /**
     * 根据公司ID查找公司，并加载其用户信息
     * @param companyId
     * @return
     */
    public Optional<Company> findByIdWithUsers(Long companyId) {
        return companyRepository.findByIdWithUsers(companyId);
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
     * 保存公司
     * @param company 公司对象
     * @return 保存后的公司对象
     */
    @CacheEvict(value = "companies", allEntries = true)
    @Transactional  // 后面逻辑错误 则会回滚
    public Company save(@Valid  @NotNull  Company company) {
        String schemaName = company.getSchemaName();
        String companyName = company.getName();

        // 一次性查询验证公司名称和schema名称的唯一性
        var existingCompanyOpt = companyRepository.findByNameAndSchemaName(
            companyName, 
            schemaName
        );

        if (existingCompanyOpt.isPresent()){
            var existingCompany = existingCompanyOpt.get();
            if (existingCompany.getName().equals(companyName)) {
                throw new BusinessException("Company name already exists: " + companyName);
            }
            if (schemaName != null && existingCompany.getSchemaName() != null && existingCompany.getSchemaName().equals(schemaName)) {
                throw new BusinessException("Schema name already exists: " + schemaName);
            }
        }


        // 验证schema名称格式以防止注入
        if (schemaName != null && !schemaName.matches(SCHEMA_NAME_PATTERN)) {
            log.error("Invalid schema name format: {}. Schema name must contain only alphanumeric characters and underscores.", schemaName);
            throw new BusinessException("Invalid schema name format: " + schemaName);
        }

        // 调用数据库函数创建schema
        try {
            Company saveCompany = companyRepository.save(company);

            Long companyId = saveCompany.getCompanyId();

            if (entityManager != null && schemaName != null) {
                // 使用参数化查询来防止SQL注入
                String sql = "SELECT public.create_or_sync_company_schema(:schemaName, :companyId)";

                Object singleResult = entityManager.createNativeQuery(sql)
                        .setParameter("schemaName", schemaName)
                        .setParameter("companyId", companyId)
                        .getSingleResult();

                log.info("Successfully created schema for company: {} ,result {} ", schemaName, singleResult);
            }
            return saveCompany;
        } catch (Exception e) {
            log.error("Failed to create schema for company: {}", schemaName, e);
            // 重新抛出异常以触发事务回滚
            throw new RuntimeException("Failed to create schema for company: " + schemaName, e);
        }
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