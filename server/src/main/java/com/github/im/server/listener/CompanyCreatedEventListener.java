package com.github.im.server.listener;

import com.github.im.server.event.CompanyCreatedEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CompanyCreatedEventListener  {
    
    private static final Logger logger = LoggerFactory.getLogger(CompanyCreatedEventListener.class);

    private final EntityManager entityManager;

    // Schema name validation regex - only allow alphanumeric characters and underscores
    private static final String SCHEMA_NAME_PATTERN = "^[a-zA-Z0-9_]+$";

    @EventListener
//    @Async
    @Transactional
    public void handleCompanyCreatedEvent(CompanyCreatedEvent event) {
        // 调用数据库函数创建schema
        try {
            String schemaName = event.getCompany().getSchemaName();
            
            // Validate schema name format
            if (schemaName == null || !schemaName.matches(SCHEMA_NAME_PATTERN)) {
                logger.error("Invalid schema name format: {}. Schema name must contain only alphanumeric characters and underscores.", schemaName);
                return;
            }
            
            if (entityManager != null) {
                Object singleResult = entityManager.createNativeQuery("SELECT create_or_sync_company_schema(?1, ?2)")
                        .setParameter(1, schemaName)
                        .setParameter(2, event.getCompany().getCompanyId())
                        .getSingleResult();
                logger.info("Successfully created schema for company: {} ,result {} ", event.getCompany().getName(),singleResult);
            }
        } catch (Exception e) {
            logger.error("Failed to create schema for company: {}", event.getCompany().getName(), e);
        }
    }
}