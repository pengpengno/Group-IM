package com.github.im.server.repository;

import com.github.im.server.model.CompanyUser;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyUserRepository extends JpaRepository<CompanyUser, Long> {
    
    List<CompanyUser> findByUserId(Long userId);
    
    List<CompanyUser> findByCompanyId(Long companyId);
    
    Optional<CompanyUser> findByUserIdAndCompanyId(Long userId, Long companyId);

    @Cacheable(value = "companies", key = "'user:companyIds:' + #userId")
    @Query("SELECT cu.companyId FROM CompanyUser cu WHERE cu.userId = ?1")
    List<Long> findCompanyIdsByUserId(Long userId);

    @Query("SELECT cu.userId FROM CompanyUser cu WHERE cu.companyId = ?1")
    List<Long> findUserIdsByCompanyId(Long companyId);
    
    List<CompanyUser> findByUserIdAndStatus(Long userId, CompanyUser.CompanyUserStatus status);
    
    List<CompanyUser> findByCompanyIdAndStatus(Long companyId, CompanyUser.CompanyUserStatus status);
    
    @Query("SELECT cu.userId FROM CompanyUser cu WHERE cu.userId IN ?1 AND cu.companyId = ?2")
    List<Long> findValidUserIdsInCompany(List<Long> userIds, Long companyId);
}