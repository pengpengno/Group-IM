package com.github.im.server.repository;

import com.github.im.server.model.Company;
import com.github.im.server.model.User;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByName(String name);

    Optional<Company> findByNameAndSchemaName(String name, String schemaName);

    @Query("SELECT c FROM Company c JOIN FETCH c.users WHERE c.companyId = :companyId")
    Optional<Company> findByIdWithUsers(@Param("companyId") Long companyId);

    Optional<Company> findBySchemaName(String schemaName);

    Optional<Company> findBySchemaNameAndActive(String schemaName, Boolean active);

    Optional<Company> findByCompanyIdAndActive(Long companyId, Boolean active);

    @Cacheable(value = "companies", key = "'company:id' + #companyId", unless = "#result == null")
    Optional<Company> findByCompanyId(Long companyId);

    @Query("SELECT u FROM User u JOIN CompanyUser cu ON u.userId = cu.userId WHERE cu.companyId = :companyId")
    List<User> findUsersByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT c FROM Company c JOIN CompanyUser cu ON c.companyId = cu.companyId WHERE cu.userId = :userId")
    List<Company> findCompaniesByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT c FROM User u JOIN u.companies c WHERE u.userId = :userId")
    List<Company> findUserCompanies(@Param("userId") Long userId);
}
