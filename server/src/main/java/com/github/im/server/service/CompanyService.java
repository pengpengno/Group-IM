package com.github.im.server.service;

import com.github.im.server.event.CompanyCreatedEvent;
import com.github.im.server.model.Company;
import com.github.im.server.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CompanyService {
    
    @Autowired
    private CompanyRepository companyRepository;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
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
}