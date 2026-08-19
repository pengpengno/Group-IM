package com.github.im.server.schema.migration.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class MigrationRuntimeConfiguration {

    @Bean(name = "schemaMigrationTaskExecutor")
    public TaskExecutor schemaMigrationTaskExecutor(
            @Value("${group.schema-migration.max-concurrency:2}") int configuredConcurrency
    ) {
        int concurrency = Math.max(1, Math.min(configuredConcurrency, 8));
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(concurrency);
        executor.setMaxPoolSize(concurrency);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("schema-migration-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
