package com.github.im.server.service

import com.github.im.server.event.CompanyCreatedEvent
import com.github.im.server.exception.BusinessException
import com.github.im.server.mapstruct.CompanyMapper
import com.github.im.server.mapstruct.UserMapper
import com.github.im.server.model.Company
import com.github.im.server.repository.CompanyRepository
import com.github.im.server.schema.migration.domain.TenantMigrationPlan
import com.github.im.server.schema.migration.provisioning.TenantSchemaProvisioner
import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import spock.lang.Specification

class CompanyServiceProvisioningSpec extends Specification {

    CompanyRepository companyRepository = Mock()
    ApplicationEventPublisher eventPublisher = Mock()
    UserMapper userMapper = Mock()
    CompanyMapper companyMapper = Mock()
    EntityManager entityManager = Mock()
    TenantSchemaProvisioner tenantSchemaProvisioner = Mock()
    CompanyProvisioningTransactionService provisioningTransactions = Mock()

    CompanyService service = new CompanyService(companyRepository, eventPublisher, userMapper, companyMapper,
            entityManager, tenantSchemaProvisioner, provisioningTransactions)

    def "new tenant remains inactive until migrations succeed"() {
        given:
        def requested = company(null, "钉钉", "dingding", true)
        def reserved = company(42L, "钉钉", "dingding", false)
        def active = company(42L, "钉钉", "dingding", true)
        def plan = new TenantMigrationPlan("dingding", "2026082002", "2026082002", 0, List.of(), false, null)

        when:
        def result = service.save(requested)

        then:
        1 * companyRepository.findByName("钉钉") >> Optional.empty()
        1 * companyRepository.findBySchemaName("dingding") >> Optional.empty()
        1 * provisioningTransactions.reserveInactive(requested) >> reserved
        1 * tenantSchemaProvisioner.provision("dingding") >> plan
        1 * provisioningTransactions.markActive(42L) >> active
        1 * eventPublisher.publishEvent({ it instanceof CompanyCreatedEvent && it.company.companyId == 42L })
        0 * provisioningTransactions.markInactive(_)
        result.active
    }

    def "failed migration preserves inactive company for explicit retry"() {
        given:
        def requested = company(null, "钉钉", "dingding", true)
        def reserved = company(42L, "钉钉", "dingding", false)
        def failure = new BusinessException(HttpStatus.CONFLICT, "MIGRATION_APPLY_FAILED", "migration failed")

        when:
        service.save(requested)

        then:
        1 * companyRepository.findByName("钉钉") >> Optional.empty()
        1 * companyRepository.findBySchemaName("dingding") >> Optional.empty()
        1 * provisioningTransactions.reserveInactive(requested) >> reserved
        1 * tenantSchemaProvisioner.provision("dingding") >> { throw failure }
        1 * provisioningTransactions.markInactive(42L) >> reserved
        0 * eventPublisher.publishEvent(_)
        def thrown = thrown(BusinessException)
        thrown.errorCode == "MIGRATION_APPLY_FAILED"
    }

    private static Company company(Long id, String name, String schema, boolean active) {
        def company = new Company()
        company.companyId = id
        company.name = name
        company.schemaName = schema
        company.active = active
        return company
    }
}
