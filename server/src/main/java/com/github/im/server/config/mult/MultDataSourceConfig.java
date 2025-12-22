package com.github.im.server.config.mult;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 多数据源配置类
 */
@Slf4j
@Configuration
public class MultDataSourceConfig {
    
    // 注入 SchemaMultiTenantConnectionProvider 并设置 DataSource
//    @Autowired
//    public void configureMultiTenantConnectionProvider(
//            SchemaMultiTenantConnectionProvider connectionProvider,
//            DataSource dataSource) {
//        connectionProvider.setDataSource(dataSource);
//    }
}