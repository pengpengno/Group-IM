package com.github.im.server.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 应用存活健康检查配置
 * 用于 Docker Compose 和 Kubernetes 的 liveness 探针
 */
@Configuration
public class HealthCheckConfig {

    /**
     * 应用存活检查
     * 只检查应用本身是否正常运行，不依赖外部服务
     */
    @Bean
    public HealthIndicator applicationLivenessHealthIndicator() {
        return () -> {
            // 简单的应用存活检查
            // 只要应用能响应，就认为存活
            return Health.up()
                .withDetail("application", "running")
                .withDetail("timestamp", System.currentTimeMillis())
                .build();
        };
    }
}
