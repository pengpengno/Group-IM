package com.github.im.server.service;

import com.github.im.dto.organization.CompanyDTO;
import com.github.im.server.mapstruct.CompanyMapper;
import com.github.im.server.model.Company;
import com.github.im.server.model.CompanyUser;
import com.github.im.server.repository.CompanyRepository;
import com.github.im.server.repository.CompanyUserRepository;
import com.github.im.server.service.CompanyService;
import com.github.im.server.util.SchemaSwitcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CompanyUserService {
    
    @Autowired
    private CompanyUserRepository companyUserRepository;
    
    @Autowired
    private CompanyService companyService;
    
    @Autowired
    private CompanyRepository companyRepository;
    
    @Autowired
    private CompanyMapper companyMapper;

    /**
     * 获取用户所属的公司列表
     * @param userId 用户ID
     * @return 公司列表
     */
//    @Cacheable(value = "companies", key = "'user:companyIds:' + #userId")

    public List<CompanyDTO> getCompanyByUserId(Long userId) {
        return  SchemaSwitcher.executeInPublicSchema(()-> {
            List<Company> companies = companyRepository.findCompaniesByUserId(userId);
            return companyMapper.companiesToCompanyDTOs(companies);
        });
    }
    
    /**
     * 获取用户关联的所有公司
     * @param userId 用户ID
     * @return 公司ID列表
     */
    public List<Long> getCompanyIdsByUserId(Long userId) {
        return companyUserRepository.findCompanyIdsByUserId(userId);
    }
    
    /**
     * 获取公司关联的所有用户
     * @param companyId 公司ID
     * @return 用户ID列表
     */
    public List<Long> getUserIdsByCompanyId(Long companyId) {
        return companyUserRepository.findUserIdsByCompanyId(companyId);
    }
    
    /**
     * 获取用户的公司关联记录
     * @param userId 用户ID
     * @return 公司用户关联列表
     */
    public List<CompanyUser> getCompaniesByUserId(Long userId) {
        return companyUserRepository.findByUserId(userId);
    }
    
    /**
     * 获取公司的用户关联记录
     * @param companyId 公司ID
     * @return 公司用户关联列表
     */
    public List<CompanyUser> getUsersByCompanyId(Long companyId) {
        return companyUserRepository.findByCompanyId(companyId);
    }
    
    /**
     * 检查用户是否属于指定公司
     * @param userId 用户ID
     * @param companyId 公司ID
     * @return 是否属于该公司
     */
    public boolean isUserInCompany(Long userId, Long companyId) {
        return companyUserRepository.findByUserIdAndCompanyId(userId, companyId).isPresent();
    }
    
    /**
     * 添加用户到公司
     * @param userId 用户ID
     * @param companyId 公司ID
     * @return 公司用户关联实体
     */
    public CompanyUser addUserToCompany(Long userId, Long companyId) {
        // 检查是否已存在关联
        Optional<CompanyUser> existing = companyUserRepository.findByUserIdAndCompanyId(userId, companyId);
        if (existing.isPresent()) {
            return existing.get();
        }
        
        CompanyUser companyUser = new CompanyUser();
        companyUser.setUserId(userId);
        companyUser.setCompanyId(companyId);
        return companyUserRepository.save(companyUser);
    }
    
    /**
     * 从公司移除用户
     * @param userId 用户ID
     * @param companyId 公司ID
     */
    public void removeUserFromCompany(Long userId, Long companyId) {
        Optional<CompanyUser> companyUser = companyUserRepository.findByUserIdAndCompanyId(userId, companyId);
        if (companyUser.isPresent()) {
            companyUserRepository.delete(companyUser.get());
        }
    }
    
    /**
     * 获取在指定公司中有效的用户ID列表
     * @param userIds 用户ID列表
     * @param companyId 公司ID
     * @return 有效的用户ID列表
     */
    public List<Long> getValidUserIdsInCompany(List<Long> userIds, Long companyId) {
        return companyUserRepository.findValidUserIdsInCompany(userIds, companyId);
    }
    
    /**
     * 更新用户在公司中的角色
     * @param userId 用户ID
     * @param companyId 公司ID
     * @param role 新角色
     * @return 更新后的公司用户关联实体
     */
    public CompanyUser updateUserRole(Long userId, Long companyId, String role) {
        Optional<CompanyUser> companyUserOpt = companyUserRepository.findByUserIdAndCompanyId(userId, companyId);
        if (companyUserOpt.isPresent()) {
            CompanyUser companyUser = companyUserOpt.get();
            return companyUserRepository.save(companyUser);
        } else {
            throw new RuntimeException("User is not associated with the company");
        }
    }
}