package com.github.im.server.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.ldap.LdapRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 应用存活健康检查测试。
 *
 * <p>该测试使用独立的最小 Spring Boot 测试应用，只验证 Actuator 健康端点，
 * 不扫描 Group-IM 业务 Service、Repository、AI 或租户组件。这样既能真实验证
 * HTTP 健康检查契约，也不要求 CI 环境提供 PostgreSQL、Redis、LDAP 或外部 AI Provider。</p>
 */
@SpringBootTest(
        classes = HealthCheckTest.HealthCheckTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "management.endpoint.health.probes.enabled=true",
                "management.endpoints.web.exposure.include=health",
                "management.health.ldap.enabled=false"
        }
)
@AutoConfigureMockMvc
class HealthCheckTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 仅用于健康检查测试的最小应用上下文。
     *
     * <p>不引用生产 {@code Application}，因此不会触发组件扫描。数据库、JPA、Redis、LDAP
     * 和安全自动配置在该测试中也不需要；健康检查本身由 Actuator 自动配置提供。</p>
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            JpaRepositoriesAutoConfiguration.class,
            RedisAutoConfiguration.class,
            LdapAutoConfiguration.class,
            LdapRepositoriesAutoConfiguration.class,
            SecurityAutoConfiguration.class,
            ManagementWebSecurityAutoConfiguration.class
    })
    static class HealthCheckTestApplication {
    }

    /**
     * Docker Compose 和 Kubernetes 使用此端点检查应用是否存活。
     */
    @Test
    void testLivenessProbe() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    /**
     * 测试基础健康检查端点。
     */
    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }
}
