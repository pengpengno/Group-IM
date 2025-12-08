package com.github.im.server.service;

import com.github.im.server.model.Company;
import com.github.im.server.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CompanyService {
    
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
     * 保存公司信息并创建对应的schema
     * @param company 公司对象
     * @return 保存后的公司对象
     */
    @Transactional
    public Company save(Company company) {
        // 保存公司信息，EntityListener会自动创建schema
        Company savedCompany = companyRepository.save(company);
        return savedCompany;
    }
    
    /**
     * 手动调用数据库函数创建schema（如果需要手动控制）
     * @param schemaName schema名称
     */
    public void createSchemaUsingFunction(String schemaName) {
        // 这个方法可以用于手动创建schema
        // 在实际使用中，通常不需要手动调用
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
    
    /**
     * 获取所有公司列表
     * @return 公司列表
     */
    public List<Company> findAll() {
        return companyRepository.findAll();
    }
}