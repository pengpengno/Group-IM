package com.github.im.server.config.mult;

import lombok.RequiredArgsConstructor;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * 多租户连接提供者实现
 * 根据不同的租户标识(schema)提供对应的数据库连接
 * 
 * 该类通过Spring管理，但在Hibernate初始化时可能通过反射创建实例，
 * 因此需要特殊处理以确保能正确获取DataSource
 */
@RequiredArgsConstructor
public class SchemaMultiTenantConnectionProvider implements MultiTenantConnectionProvider<String> {
    
    // 使用 volatile 确保线程安全
    private final DataSource dataSource;

    /**
     * 获取任意连接
     * 
     * @return 数据库连接
     * @throws SQLException SQL异常
     */
    @Override
    public Connection getAnyConnection() throws SQLException {
        Objects.requireNonNull(dataSource, "DataSource is not initialized yet");
        return dataSource.getConnection();
    }

    /**
     * 释放任意连接
     * 
     * @param connection 数据库连接
     * @throws SQLException SQL异常
     */
    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    /**
     * 获取指定租户的连接
     * 
     * 根据租户标识设置数据库连接的schema，确保操作在正确的租户数据上进行
     * 
     * @param tenantIdentifier 租户标识(schema名称)
     * @return 数据库连接
     * @throws SQLException SQL异常
     */
    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        Objects.requireNonNull(dataSource, "DataSource is not initialized yet");
        final Connection connection = dataSource.getConnection();
        
        // 如果没有指定schema，则使用默认的public schema
        if (tenantIdentifier == null || tenantIdentifier.isEmpty()) {
            connection.setSchema("public");
        } else {
            // 设置当前连接的schema
            connection.setSchema(tenantIdentifier);
        }
        
        return connection;
    }

    /**
     * 释放指定租户的连接
     * 
     * @param tenantIdentifier 租户标识
     * @param connection 数据库连接
     * @throws SQLException SQL异常
     */
    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        // 重置schema为public
        connection.setSchema("public");
        connection.close();
    }

    /**
     * 是否支持积极释放连接
     * 
     * @return false表示不支持积极释放
     */
    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    /**
     * 是否可以解包为指定类型
     * 
     * @param unwrapType 解包类型
     * @return 是否支持解包
     */
    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return false;
    }

    /**
     * 解包为指定类型
     * 
     * @param unwrapType 解包类型
     * @param <T> 类型参数
     * @return 解包后的对象
     */
    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        return null;
    }
}