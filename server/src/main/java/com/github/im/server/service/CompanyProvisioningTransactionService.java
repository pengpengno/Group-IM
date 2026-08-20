package com.github.im.server.service;

import com.github.im.server.exception.BusinessException;
import com.github.im.server.model.Company;
import com.github.im.server.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyProvisioningTransactionService {

    private final CompanyRepository companyRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Company reserveInactive(Company company) {
        company.setActive(false);
        return companyRepository.saveAndFlush(company);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Company markActive(Long companyId) {
        Company company = requireCompany(companyId);
        company.setActive(true);
        return companyRepository.saveAndFlush(company);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Company markInactive(Long companyId) {
        Company company = requireCompany(companyId);
        company.setActive(false);
        return companyRepository.saveAndFlush(company);
    }

    private Company requireCompany(Long companyId) {
        return companyRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "COMPANY_NOT_FOUND",
                        "Company not found: " + companyId
                ));
    }
}
