package com.github.im.server.config.mult;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * 租户上下文持有者
 * 使用TransmittableThreadLocal确保在线程池环境下也能正确传递租户上下文
 * 
 * TransmittableThreadLocal是阿里巴巴开源的ThreadLocal扩展，解决了线程池中ThreadLocal值传递的问题。
 * 当使用线程池等会缓存线程的组件时，ThreadLocal值无法正确传递到子线程，
 * TransmittableThreadLocal可以解决这个问题，确保上下文在父子线程间正确传递。
 * 
 * 注意：在使用{@link org.springframework.transaction.annotation.Transactional}注解的事务中，
 * 直接切换schema可能会失效，因为事务开启时已经确定了数据库连接和schema，
 * 此后对SchemaContext的修改不会影响已存在的数据库连接。
 * 如果需要在事务中切换schema，请使用{@link com.github.im.server.util.SchemaSwitcher}提供的
 * {@code executeWithFreshConnectionInSchema} 或 {@code executeInSchemaWithTransaction} 方法。
 */
public class SchemaContext {
    
    /**
     * 使用TransmittableThreadLocal存储当前租户标识
     * 确保在线程池等会缓存线程的环境下，租户上下文也能正确传递
     */
    private static final TransmittableThreadLocal<String> CURRENT_TENANT = new TransmittableThreadLocal<>();

    /**
     * 设置当前租户标识
     * 
     * @param schemaName 租户标识(schema名称)
     */
    public static void setCurrentTenant(String schemaName) {
        CURRENT_TENANT.set(schemaName);
    }

    /**
     * 获取当前租户标识
     * 
     * @return 当前租户标识，如果未设置则返回null
     */
    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    /**
     * 清除当前租户标识
     * 在请求处理完毕后调用，防止内存泄漏
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }

}