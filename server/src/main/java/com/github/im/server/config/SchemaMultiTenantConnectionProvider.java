package com.github.im.server.config;

import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 多租户连接提供者实现
 * 根据不同的租户标识(schema)提供对应的数据库连接
 */
@Component
public class SchemaMultiTenantConnectionProvider extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl {

    @Autowired
    private DataSource dataSource;

    /**
     * 选择任意数据源
     * @return 数据源
     */
    @Override
    protected DataSource selectAnyDataSource() {
        return dataSource;
    }

    /**
     * 根据租户标识选择数据源
     * @param tenantIdentifier 租户标识
     * @return 数据源
     */
    @Override
    protected DataSource selectDataSource(Object tenantIdentifier) {
        return dataSource;
    }

    /**
     * 获取指定schema的连接
     * @param tenantIdentifier 租户标识(schema名称)
     * @return 数据库连接
     * @throws SQLException SQL异常
     */
    @Override
    public Connection getConnection(Object tenantIdentifier) throws SQLException {
        final Connection connection = dataSource.getConnection();
        
        // 如果没有指定schema，则使用默认的public schema
        if (tenantIdentifier == null || tenantIdentifier.toString().isEmpty()) {
            connection.setSchema("public");
        } else {
            // 设置当前连接的schema
            connection.setSchema(tenantIdentifier.toString());
        }
        
        return connection;
    }
}