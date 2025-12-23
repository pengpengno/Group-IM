package com.github.im.server.security;

import com.github.im.server.model.Company;
import com.github.im.server.model.User;
import com.github.im.server.repository.CompanyRepository;
import com.github.im.server.repository.UserRepository;
import com.github.im.server.service.CompanyUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("companyOwnershipChecker")
@RequiredArgsConstructor
public class CompanyOwnershipChecker {

    private final UserRepository userRepository;
    private final CompanyUserService companyUserService;
    private final CompanyRepository companyRepository;


    /**
     * 检查用户是否拥有指定公司的访问权限
     * @param user
     * @param companyId
     * @return
     */
    public boolean hasCompanyAccess(final User user, final Long companyId) {
        List<Company> companies = user.getCompanies();
        if (companies == null || companies.isEmpty()) {
            // 获取用户所有的公司
            companies = companyRepository.findCompaniesByUserId(user.getUserId());
        }
        return companies.stream()
                .anyMatch(c -> c.getCompanyId().equals(companyId));
    }

    public boolean isCurrentCompany(User user, Long companyId) {
        return user.getCurrentCompany() != null
                && user.getCurrentCompany().getCompanyId().equals(companyId);
    }

}