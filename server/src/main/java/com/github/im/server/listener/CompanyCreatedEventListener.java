package com.github.im.server.listener;

import com.github.im.server.event.CompanyCreatedEvent;
import com.github.im.server.model.Company;
import com.github.im.server.repository.CompanyRepository;
import com.github.im.server.service.CompanyService;
import com.github.im.server.web.ApiResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CompanyCreatedEventListener  {
    
    private static final Logger logger = LoggerFactory.getLogger(CompanyCreatedEventListener.class);

    private final EntityManager entityManager;


    private final CompanyRepository companyRepository;
    
    // Schema name validation regex - only allow alphanumeric characters and underscores
    private static final String SCHEMA_NAME_PATTERN = "^[a-zA-Z0-9_]+$";

    @EventListener
//    @Async
    @Transactional
    public void handleCompanyCreatedEvent(CompanyCreatedEvent event) {
        // 调用数据库函数创建schema
        try {
            String schemaName = event.getCompany().getSchemaName();
            
            // Validate schema name format to prevent injection
            if (schemaName == null || !schemaName.matches(SCHEMA_NAME_PATTERN)) {
                logger.error("Invalid schema name format: {}. Schema name must contain only alphanumeric characters and underscores.", schemaName);
                throw new IllegalArgumentException("Invalid schema name format: " + schemaName);
            }
            
            Long companyId = event.getCompany().getCompanyId();
            
            if (entityManager != null) {
                // 使用参数化查询来防止SQL注入
                String sql = "SELECT public.create_or_sync_company_schema(:schemaName, :companyId)";
                
                Object singleResult = entityManager.createNativeQuery(sql)
                    .setParameter("schemaName", schemaName)
                    .setParameter("companyId", companyId)
                    .getSingleResult();
                
                logger.info("Successfully created schema for company: {} ,result {} ", event.getCompany().getName(), singleResult);
            }
        } catch (Exception e) {
            logger.error("Failed to create schema for company: {}", event.getCompany().getName(), e);
            // 重新抛出异常以触发事务回滚
            throw new RuntimeException("Failed to create schema for company: " + event.getCompany().getName(), e);
        }
    }
}