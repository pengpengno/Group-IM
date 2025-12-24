package com.github.im.server.repository;

import com.github.im.server.model.Company;
import com.github.im.server.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    
    /**
     * 根据公司名称查找公司
     * @param name 公司名称
     * @return 公司对象
     */
    Optional<Company> findByName(String name);

    Optional<Company> findByNameAndSchemaName(String name,String schemaName);


    /**
     * 根据公司ID查找公司，并加载其用户信息
     * @param companyId 公司ID
     * @return 公司对象
     */
    @Query("SELECT c FROM Company c JOIN FETCH c.users WHERE c.companyId = :companyId")
    Optional<Company> findByIdWithUsers(@Param("companyId") Long companyId);


    /**
     * 根据schema名称查找公司
     * @param schemaName schema名称
     * @return 公司对象
     */
    @Cacheable(value = "companies", key = "'company:schema:' + #schemaName")
    Optional<Company> findBySchemaName(String schemaName);
    
    /**
     * 根据公司ID和激活状态查找公司
     * @param companyId 公司ID
     * @param active 激活状态
     * @return 公司对象
     */
    @Cacheable(value = "companies", key = "'company:id:' + #companyId + ':active:' + #active")
    Optional<Company> findByCompanyIdAndActive(Long companyId, Boolean active);
    
    /**
     * 根据公司ID查找公司
     * @param companyId 公司ID
     * @return 公司对象
     */
    @Cacheable(value = "companies", key = "'company:id:' + #companyId")
    Optional<Company> findByCompanyId(Long companyId);
    

    
    @Query("SELECT u FROM User u JOIN CompanyUser cu ON u.userId = cu.userId WHERE cu.companyId = :companyId")
    List<User> findUsersByCompanyId(@Param("companyId") Long companyId);
    
    @Query("SELECT c FROM Company c JOIN CompanyUser cu ON c.companyId = cu.companyId WHERE cu.userId = :userId")
    @Cacheable(value = "companies", key = "'user:companyIds:' + #userId")
    List<Company> findCompaniesByUserId(@Param("userId") Long userId);
    
    @Query("SELECT DISTINCT c FROM User u JOIN u.companies c WHERE u.userId = :userId")
    List<Company> findUserCompanies(@Param("userId") Long userId);
}