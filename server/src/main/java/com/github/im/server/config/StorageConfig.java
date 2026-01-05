package com.github.im.server.config;

import com.github.im.server.service.storage.LocalStorageStrategy;
import com.github.im.server.service.storage.StorageStrategy;
import com.github.im.server.service.storage.StorageStrategyFactory;
import com.github.im.server.config.FileUploadProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
@RequiredArgsConstructor
public class StorageConfig {

    private final FileUploadProperties fileUploadProperties;

    @Bean
    public StorageStrategyFactory storageStrategyFactory() {
        StorageStrategyFactory factory = new StorageStrategyFactory();
        return factory;
    }

    @Bean
    @ConditionalOnProperty(name = "group.storage.type", havingValue = "local", matchIfMissing = true)
    public StorageStrategy localStorageStrategy() {
        Path baseDir = fileUploadProperties.getBasePath().startsWith("/") ?
                java.nio.file.Paths.get(fileUploadProperties.getBasePath()) :
                java.nio.file.Paths.get(System.getProperty("user.dir")).resolve(fileUploadProperties.getBasePath());
        return new LocalStorageStrategy(baseDir, file -> {
            try (java.io.InputStream is = file.getInputStream()) {
                return org.springframework.util.DigestUtils.md5DigestAsHex(is);
            } catch (java.io.IOException e) {
                throw new RuntimeException("Failed to calculate file hash", e);
            }
        });
    }
}