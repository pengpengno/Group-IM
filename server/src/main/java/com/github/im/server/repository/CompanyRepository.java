package com.github.im.server.repository;

import com.github.im.server.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    
    /**
     * 根据公司名称查找公司
     * @param name 公司名称
     * @return 公司对象
     */
    Optional<Company> findByName(String name);
    
    /**
     * 根据schema名称查找公司
     * @param schemaName schema名称
     * @return 公司对象
     */
    Optional<Company> findBySchemaName(String schemaName);
    
    /**
     * 根据公司ID和激活状态查找公司
     * @param companyId 公司ID
     * @param active 激活状态
     * @return 公司对象
     */
    Optional<Company> findByCompanyIdAndActive(Long companyId, Boolean active);
}