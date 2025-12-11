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


    @EventListener
//    @Async
    @Transactional
    public void handleCompanyCreatedEvent(CompanyCreatedEvent event) {
        // 调用数据库函数创建schema
        try {
            if (entityManager != null) {
                Object singleResult = entityManager.createNativeQuery("SELECT create_or_sync_company_schema(?1)")
                        .setParameter(1, event.getCompany().getSchemaName())
                        .getSingleResult();//                        .executeUpdate()
                logger.info("Successfully created schema for company: {} ,result {} ", event.getCompany().getName(),singleResult);
            }
        } catch (Exception e) {
            logger.error("Failed to create schema for company: {}", event.getCompany().getName(), e);
        }
    }
}