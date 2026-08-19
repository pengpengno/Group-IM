package com.github.im.server.schema.migration.security;

import com.github.im.server.exception.BusinessException;
import com.github.im.server.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Migration API authorization bridge.
 *
 * <p>The repository does not yet expose a real SYSTEM_ADMIN authority. Until RBAC is introduced,
 * migration endpoints centralize the legacy configured-admin check here instead of scattering
 * username comparisons across controllers. This component is the single replacement point for
 * a future authority/permission service.</p>
 */
@Component
public class MigrationAdminAuthorizer {
    private final String configuredAdminUsername;

    public MigrationAdminAuthorizer(
            @Value("${group.system.initializer.admin-user.username:admin}") String configuredAdminUsername
    ) {
        this.configuredAdminUsername = configuredAdminUsername;
    }

    public void requireAdmin(User user) {
        if (user == null || user.getUsername() == null || !Objects.equals(user.getUsername(), configuredAdminUsername)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "MIGRATION_FORBIDDEN",
                    "无权限执行 tenant schema migration");
        }
    }
}
