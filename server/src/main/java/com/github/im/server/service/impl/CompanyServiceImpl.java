package com.github.im.server.service.impl;

import com.github.im.server.model.Company;
import com.github.im.server.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CompanyServiceImpl {
    
    @Autowired
    private CompanyRepository companyRepository;
    
    /**
     * 根据公司ID查找公司
     * @param companyId 公司ID
     * @return 公司对象
     */
    public Optional<Company> findById(Long companyId) {
        return companyRepository.findById(companyId);
    }


    /**
     * 注册公司
     * @param name 公司名称
     * @param schemaName schema名称
     * @return 注册成功的公司对象
     */
    public Optional<Company> registerCompany(String name, String schemaName) {
        // 检查公司名称是否已存在
        Optional<Company> existingCompany = companyRepository.findByName(name);
        if (existingCompany.isPresent()) {
            return Optional.empty();
        }

        // 检查schema名称是否已存在
        Optional<Company> existingSchema = companyRepository.findBySchemaName(schemaName);
        if (existingSchema.isPresent()) {
            return Optional.empty();
        }

        // 创建新的公司对象
        Company company = new Company(name, schemaName);
        return Optional.of(companyRepository.save(company));
    }
    
    /**
     * 根据公司名称查找公司
     * @param name 公司名称
     * @return 公司对象
     */
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
     * 保存公司信息
     * @param company 公司对象
     * @return 保存后的公司对象
     */
    public Company save(Company company) {
        return companyRepository.save(company);
    }
    
    /**
     * 根据公司ID查找对应的schema名称
     * @param companyId 公司ID
     * @return schema名称
     */
    public String getSchemaNameByCompanyId(Long companyId) {
        return companyRepository.findById(companyId)
                .map(Company::getSchemaName)
                .orElse("company_" + companyId);
    }
}