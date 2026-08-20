package com.github.im.server.service;

import com.github.im.dto.organization.CompanyDTO;
import com.github.im.dto.user.UserInfo;
import com.github.im.server.event.CompanyCreatedEvent;
import com.github.im.server.exception.BusinessException;
import com.github.im.server.mapstruct.CompanyMapper;
import com.github.im.server.mapstruct.UserMapper;
import com.github.im.server.model.Company;
import com.github.im.server.model.User;
import com.github.im.server.repository.CompanyRepository;
import com.github.im.server.schema.migration.provisioning.TenantSchemaProvisioner;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Validated
@Slf4j
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserMapper userMapper;
    private final CompanyMapper companyMapper;
    private final EntityManager entityManager;
    private final TenantSchemaProvisioner tenantSchemaProvisioner;
    private final CompanyProvisioningTransactionService provisioningTransactions;

    private static final String SCHEMA_NAME_PATTERN = "^[a-zA-Z0-9_]+$";

    public Optional<Company> findById(Long companyId) {
        return companyRepository.findByCompanyId(companyId);
    }

    public Optional<Company> findByIdWithUsers(Long companyId) {
        return companyRepository.findByIdWithUsers(companyId);
    }

    public Optional<Company> findBySchemaName(String schemaName) {
        return companyRepository.findBySchemaName(schemaName);
    }

    /**
     * Save company metadata. A newly-created non-public tenant is reserved as inactive,
     * provisioned through Flyway, and only then activated.
     */
    @CacheEvict(value = "companies", allEntries = true)
    public Company save(@Valid @NotNull Company company) {
        validateCompany(company);

        boolean isNew = company.getCompanyId() == null;
        String schemaName = company.getSchemaName();
        if (!isNew || "public".equalsIgnoreCase(schemaName)) {
            return companyRepository.save(company);
        }

        boolean activateAfterProvisioning = !Boolean.FALSE.equals(company.getActive());
        Company reserved = provisioningTransactions.reserveInactive(company);
        try {
            var migrationPlan = tenantSchemaProvisioner.provision(schemaName);
            Company provisioned = activateAfterProvisioning
                    ? provisioningTransactions.markActive(reserved.getCompanyId())
                    : provisioningTransactions.markInactive(reserved.getCompanyId());
            eventPublisher.publishEvent(new CompanyCreatedEvent(provisioned));
            log.info(
                    "Provisioned new company {} with schema {} at tenant migration version {} (active={})",
                    provisioned.getCompanyId(),
                    schemaName,
                    migrationPlan.currentVersion(),
                    provisioned.getActive()
            );
            return provisioned;
        } catch (RuntimeException exception) {
            keepCompanyInactive(reserved.getCompanyId(), exception);
            log.error("Failed to provision tenant schema {} for company {}", schemaName, reserved.getCompanyId(), exception);
            throw exception;
        }
    }

    public CompanyDTO registerCompany(CompanyDTO companyDTO) {
        Company company = companyMapper.companyDTOToCompany(companyDTO);
        if (company.getActive() == null) {
            company.setActive(true);
        }
        return companyMapper.companyToCompanyDTO(save(company));
    }

    /**
     * Explicit retry for a company whose metadata reservation succeeded but provisioning did not.
     * Active companies are intentionally rejected so legacy/non-empty tenants cannot silently enter
     * the new-company path.
     */
    @CacheEvict(value = "companies", allEntries = true)
    public CompanyDTO retryProvisioning(Long companyId) {
        Company company = companyRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "COMPANY_NOT_FOUND",
                        "Company not found: " + companyId
                ));

        if (Boolean.TRUE.equals(company.getActive())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "COMPANY_ALREADY_ACTIVE",
                    "Company is already active and cannot use new-tenant provisioning retry: " + companyId
            );
        }
        if ("public".equalsIgnoreCase(company.getSchemaName())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "COMPANY_PUBLIC_NOT_TENANT",
                    "public company is not a tenant provisioning target"
            );
        }

        try {
            var migrationPlan = tenantSchemaProvisioner.provision(company.getSchemaName());
            Company activated = provisioningTransactions.markActive(companyId);
            eventPublisher.publishEvent(new CompanyCreatedEvent(activated));
            log.info(
                    "Retried tenant provisioning for company {} schema {} at version {}",
                    companyId,
                    company.getSchemaName(),
                    migrationPlan.currentVersion()
            );
            return companyMapper.companyToCompanyDTO(activated);
        } catch (RuntimeException exception) {
            keepCompanyInactive(companyId, exception);
            throw exception;
        }
    }

    public List<CompanyDTO> getAllCompanies() {
        List<Company> companies = companyRepository.findAll();
        return companyMapper.companiesToCompanyDTOs(companies);
    }

    public String getSchemaNameByCompanyId(Long companyId) {
        return findById(companyId).map(Company::getSchemaName).orElse("public");
    }

    public List<UserInfo> getUsersByCompanyId(Long companyId) {
        List<User> users = companyRepository.findUsersByCompanyId(companyId);
        if (users.isEmpty()) {
            return new ArrayList<>();
        }
        return userMapper.usersToUserInfos(users);
    }

    public List<CompanyDTO> getCompaniesByIds(List<Long> companyIds) {
        if (companyIds == null || companyIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Company> companies = companyRepository.findAllById(companyIds);
        return companyMapper.companiesToCompanyDTOs(companies);
    }

    /**
     * Legacy compatibility operation. This explicit admin path is intentionally retained for now,
     * but new-company provisioning no longer calls it.
     */
    @Transactional
    public void syncSchemas(List<Long> companyIds) {
        List<Company> companies;
        if (companyIds == null || companyIds.isEmpty()) {
            companies = companyRepository.findAll();
        } else {
            companies = companyRepository.findAllById(companyIds);
        }

        for (Company company : companies) {
            String schemaName = company.getSchemaName();
            Long companyId = company.getCompanyId();

            if (schemaName != null && !schemaName.equalsIgnoreCase("public")) {
                try {
                    String sql = "SELECT public.create_or_sync_company_schema(:schemaName, :companyId)";
                    Object singleResult = entityManager.createNativeQuery(sql)
                            .setParameter("schemaName", schemaName)
                            .setParameter("companyId", companyId)
                            .getSingleResult();
                    log.info("Successfully synced schema for company {}: result {}", schemaName, singleResult);
                } catch (Exception exception) {
                    log.error("Failed to sync schema for company {}: {}", schemaName, exception.getMessage());
                }
            }
        }
    }

    private void validateCompany(Company company) {
        String schemaName = company.getSchemaName();
        String companyName = company.getName();

        if (companyName == null || companyName.isBlank()) {
            throw new BusinessException("Company name must not be blank");
        }
        if (schemaName == null || schemaName.isBlank() || !schemaName.matches(SCHEMA_NAME_PATTERN)) {
            log.error("Invalid schema name format: {}", schemaName);
            throw new BusinessException("Invalid schema name format: " + schemaName);
        }

        companyRepository.findByName(companyName).ifPresent(existing -> {
            if (!Objects.equals(existing.getCompanyId(), company.getCompanyId())) {
                throw new BusinessException("Company name already exists: " + companyName);
            }
        });
        companyRepository.findBySchemaName(schemaName).ifPresent(existing -> {
            if (!Objects.equals(existing.getCompanyId(), company.getCompanyId())) {
                throw new BusinessException("Schema name already exists: " + schemaName);
            }
        });
    }

    private void keepCompanyInactive(Long companyId, RuntimeException rootCause) {
        try {
            provisioningTransactions.markInactive(companyId);
        } catch (RuntimeException stateException) {
            rootCause.addSuppressed(stateException);
            log.error("Failed to preserve inactive state for company {} after provisioning failure", companyId, stateException);
        }
    }
}
