package com.github.im.server.schema.migration.baseline;

import com.github.im.server.exception.BusinessException;
import com.github.im.server.schema.migration.service.TenantFlywayFactory;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Selects the expected core-schema contract for a tenant.
 *
 * No-history tenants always use the immutable 2026081906 adoption contract.
 * History-managed tenants must first pass Flyway validation so a later managed
 * contract cannot be claimed by merely editing the database schema by hand.
 */
@Component
public class ManagedCoreSchemaContractResolver {

    private final TenantFlywayFactory flywayFactory;

    public ManagedCoreSchemaContractResolver(TenantFlywayFactory flywayFactory) {
        this.flywayFactory = flywayFactory;
    }

    public ManagedCoreSchemaContract resolve(String schemaName, boolean historyExists) {
        if (!historyExists) {
            return ManagedCoreSchemaContract.adoptionBaseline();
        }

        try {
            Flyway flyway = flywayFactory.create(schemaName);
            flyway.validate();
            MigrationInfo current = flyway.info().current();
            if (current == null || current.getVersion() == null) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "MIGRATION_MANAGED_CORE_VERSION_UNKNOWN",
                        "tenant 已有 Flyway history，但无法确定当前成功版本: " + schemaName
                );
            }
            return ManagedCoreSchemaContract.forManagedVersion(current.getVersion().getVersion());
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "MIGRATION_MANAGED_CORE_HISTORY_INVALID",
                    "tenant Flyway history/checksum 校验失败，不能接受 managed core evolution: " + safeMessage(exception)
            );
        }
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
