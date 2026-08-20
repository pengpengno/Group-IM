package com.github.im.server.workbench.common.context;

import com.github.im.server.config.mult.SchemaContext;
import com.github.im.server.model.Company;
import com.github.im.server.model.User;
import com.github.im.server.workbench.common.error.WorkbenchException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityCurrentWorkContextProviderTest {

    private final SecurityCurrentWorkContextProvider provider = new SecurityCurrentWorkContextProvider();

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        SchemaContext.clear();
    }

    @Test
    void resolvesAuthenticatedUserCompanyAndSchema() {
        User user = authenticatedUser(42L, 7L, "tenant_a", true);
        SchemaContext.setCurrentTenant("tenant_a");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, "n/a", user.getAuthorities())
        );

        CurrentWorkContext context = provider.require();

        assertEquals(42L, context.userId());
        assertEquals(7L, context.companyId());
        assertEquals("tenant_a", context.schemaName());
    }

    @Test
    void rejectsTenantContextMismatch() {
        User user = authenticatedUser(42L, 7L, "tenant_a", true);
        SchemaContext.setCurrentTenant("tenant_b");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, "n/a", user.getAuthorities())
        );

        WorkbenchException exception = assertThrows(WorkbenchException.class, provider::require);
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("WORKBENCH_TENANT_CONTEXT_MISMATCH", exception.getErrorCode());
    }

    @Test
    void rejectsInactiveCurrentCompany() {
        User user = authenticatedUser(42L, 7L, "tenant_a", false);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, "n/a", user.getAuthorities())
        );

        WorkbenchException exception = assertThrows(WorkbenchException.class, provider::require);
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("WORKBENCH_COMPANY_INACTIVE", exception.getErrorCode());
    }

    @Test
    void rejectsMissingAuthentication() {
        WorkbenchException exception = assertThrows(WorkbenchException.class, provider::require);
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    private User authenticatedUser(Long userId, Long companyId, String schemaName, boolean active) {
        Company company = new Company("Company " + companyId, schemaName);
        company.setCompanyId(companyId);
        company.setActive(active);

        User user = new User();
        user.setUserId(userId);
        user.setUsername("user" + userId);
        user.setCurrentCompany(company);
        return user;
    }
}
