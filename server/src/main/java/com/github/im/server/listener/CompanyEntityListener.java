package com.github.im.server.listener;

import com.github.im.server.model.Company;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PostPersist;
import org.springframework.stereotype.Component;

/**
 * Company实体监听器
 * 当Company实体被持久化后，自动创建对应的数据库schema
 */
@Component
public class CompanyEntityListener {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    /**
     * 每当添加了恶一个 公司  那么久创建一个 新的 schema ，同时将 现有的schema结构都 COPY 过去
     * @param company 公司
     */
    @PostPersist
    public void onCreate(Company company) {
        // 调用数据库函数创建schema
        if (entityManager != null) {
            entityManager.createNativeQuery("SELECT create_company_schema(?1)")
                .setParameter(1, company.getSchemaName())
                .executeUpdate();
        }
    }
}