package com.github.im.server.util;

import com.github.im.server.config.mult.SchemaContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;
import java.util.function.Function;

/**
 * Schema切换工具类
 * 提供便捷的方法在不同schema之间切换，特别是解决在public和公司schema之间频繁切换的场景
 * 
 * 推荐使用executeWithFreshConnectionInSchema方法来确保操作在正确的schema中执行，
 * 因为它会创建新的数据库连接并设置正确的schema，避免Hibernate连接缓存问题。
 */
@Slf4j
@UtilityClass
public class SchemaSwitcher {

    /**
     * 在指定schema中执行操作，自动管理SchemaContext
     * 此方法会自动设置和清理SchemaContext
     *
     * @param schemaName schema名称
     * @param operation 要执行的操作
     * @return 操作结果
     * @param <T> 返回值类型
     */
    public static <T> T executeInSchema(String schemaName, Supplier<T> operation) {
        String previousSchema = SchemaContext.getCurrentTenant();
        try {
            SchemaContext.setCurrentTenant(schemaName);
            return operation.get();
        } finally {
            if (previousSchema != null) {
                SchemaContext.setCurrentTenant(previousSchema);
            } else {
                SchemaContext.clear();
            }
        }
    }

    /**
     * 在指定schema中执行操作（无返回值），自动管理SchemaContext
     * 此方法会自动设置和清理SchemaContext
     *
     * @param schemaName schema名称
     * @param operation 要执行的操作
     */
    public static void executeInSchema(String schemaName, Runnable operation) {
        String previousSchema = SchemaContext.getCurrentTenant();
        try {
            SchemaContext.setCurrentTenant(schemaName);
            operation.run();
        } finally {
            if (previousSchema != null) {
                SchemaContext.setCurrentTenant(previousSchema);
            } else {
                SchemaContext.clear();
            }
        }
    }

    /**
     * 在public schema中执行操作，自动管理SchemaContext
     * 这是针对频繁切换到public schema场景的便捷方法
     *
     * @param operation 要执行的操作
     * @return 操作结果
     * @param <T> 返回值类型
     */
    public static <T> T executeInPublicSchema(Supplier<T> operation) {
        return executeInSchema("public", operation);
    }

    /**
     * 在public schema中执行操作（无返回值），自动管理SchemaContext
     * 这是针对频繁切换到public schema场景的便捷方法
     *
     * @param operation 要执行的操作
     */
    public static void executeInPublicSchema(Runnable operation) {
        executeInSchema("public", operation);
    }

    /**
     * 使用全新的连接在指定schema中执行操作
     * 这种方式会获取一个新的数据库连接并设置schema，确保操作在正确的schema中执行
     * 这是推荐的方式，特别是在需要确保操作在特定schema中执行的场景下
     * JPA HIB 无法处理 不同schema下的事务处理;如果使用了 {@link org.springframework.transaction.annotation.Transactional } 在同一事务下切换schema 的逻辑无法生效
     * @param entityManager 实体管理器
     * @param schemaName schema名称
     * @param operation 要执行的操作
     * @return 操作结果
     */
    public static <T> T executeWithFreshConnectionInSchema(EntityManager entityManager, String schemaName, 
            Function<EntityManager, T> operation) {
        EntityManagerFactory emf = entityManager.getEntityManagerFactory();
        EntityManager freshEntityManager = null;
        try {
            freshEntityManager = emf.createEntityManager();
            
            // 尝试获取Session并使用doWork设置schema
            try {
                Session session = freshEntityManager.unwrap(Session.class);
                session.doWork(connection -> {
                    connection.setSchema(schemaName);
                });
            } catch (Exception e) {
                log.warn("无法unwrap到Session或设置schema失败，将通过原生SQL设置schema");
                // 如果无法unwrap到Session，则通过执行原生SQL设置schema
                freshEntityManager.createNativeQuery("SET LOCAL search_path TO " + schemaName).executeUpdate();
            }
            
            // 开启事务
            freshEntityManager.getTransaction().begin();
            
            try {
                // 执行操作
                T result = operation.apply(freshEntityManager);
                
                // 提交事务
                freshEntityManager.getTransaction().commit();
                return result;
            } catch (Exception e) {
                // 回滚事务
                try {
                    freshEntityManager.getTransaction().rollback();
                } catch (Exception rollbackEx) {
                    log.warn("事务回滚失败", rollbackEx);
                }
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException("在schema " + schemaName + "中执行操作失败", e);
        } finally {
            // 关闭EntityManager
            if (freshEntityManager != null && freshEntityManager.isOpen()) {
                try {
                    freshEntityManager.close();
                } catch (Exception e) {
                    log.warn("关闭EntityManager时发生异常", e);
                }
            }
        }
    }
    
    /**
     * 使用全新的连接在指定schema中执行操作（无返回值）
     * 
     * @param entityManager 实体管理器
     * @param schemaName schema名称
     * @param operation 要执行的操作
     */
    public static void executeWithFreshConnectionInSchema(EntityManager entityManager, String schemaName, 
            java.util.function.Consumer<EntityManager> operation) {
        EntityManagerFactory emf = entityManager.getEntityManagerFactory();
        EntityManager freshEntityManager = null;
        try {
            freshEntityManager = emf.createEntityManager();
            
            // 尝试获取Session并使用doWork设置schema
            try {
                Session session = freshEntityManager.unwrap(Session.class);
                session.doWork(connection -> {
                    connection.setSchema(schemaName);
                });
            } catch (Exception e) {
                log.warn("无法unwrap到Session或设置schema失败，将通过原生SQL设置schema");
                // 如果无法unwrap到Session，则通过执行原生SQL设置schema
                freshEntityManager.createNativeQuery("SET LOCAL search_path TO " + schemaName).executeUpdate();
            }
            
            // 开启事务
            freshEntityManager.getTransaction().begin();
            
            try {
                // 执行操作
                operation.accept(freshEntityManager);
                
                // 提交事务
                freshEntityManager.getTransaction().commit();
            } catch (Exception e) {
                // 回滚事务
                try {
                    freshEntityManager.getTransaction().rollback();
                } catch (Exception rollbackEx) {
                    log.warn("事务回滚失败", rollbackEx);
                }
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException("在schema " + schemaName + "中执行操作失败", e);
        } finally {
            // 关闭EntityManager
            if (freshEntityManager != null && freshEntityManager.isOpen()) {
                try {
                    freshEntityManager.close();
                } catch (Exception e) {
                    log.warn("关闭EntityManager时发生异常", e);
                }
            }
        }
    }
    
    /**
     * 使用Spring事务管理器在指定schema中执行操作
     * 这是一个更高效的方法，因为它利用了Spring的事务管理和连接池
     * 
     * @param transactionManager 事务管理器
     * @param entityManager 实体管理器
     * @param schemaName schema名称
     * @param operation 要执行的操作
     * @return 操作结果
     */
    public static <T> T executeInSchemaWithTransaction(PlatformTransactionManager transactionManager, 
            EntityManager entityManager, String schemaName, Function<EntityManager, T> operation) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        
        return transactionTemplate.execute(status -> {
            try {
                // 获取连接并设置schema
                Connection connection = entityManager.unwrap(Connection.class);
                String previousSchema = connection.getSchema();
                connection.setSchema(schemaName);
                
                try {
                    // 执行操作
                    return operation.apply(entityManager);
                } finally {
                    // 恢复原来的schema
                    if (previousSchema != null) {
                        connection.setSchema(previousSchema);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("在schema " + schemaName + "中执行操作失败", e);
            }
        });
    }
    
    /**
     * 使用Spring事务管理器在指定schema中执行操作（无返回值）
     * 
     * @param transactionManager 事务管理器
     * @param entityManager 实体管理器
     * @param schemaName schema名称
     * @param operation 要执行的操作
     */
    public static void executeInSchemaWithTransaction(PlatformTransactionManager transactionManager,
            EntityManager entityManager, String schemaName, java.util.function.Consumer<EntityManager> operation) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        
        transactionTemplate.executeWithoutResult(status -> {
            try {
                // 获取连接并设置schema
                Connection connection = entityManager.unwrap(Connection.class);
                String previousSchema = connection.getSchema();
                connection.setSchema(schemaName);
                
                try {
                    // 执行操作
                    operation.accept(entityManager);
                } finally {
                    // 恢复原来的schema
                    if (previousSchema != null) {
                        connection.setSchema(previousSchema);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("在schema " + schemaName + "中执行操作失败", e);
            }
        });
    }
    
    /**
     * 在指定的schema中执行原生SQL查询，不改变当前线程的schema上下文
     * 
     * @param entityManager 实体管理器
     * @param schemaName schema名称
     * @param sql 原生SQL查询语句
     * @param clazz 返回结果类型
     * @return 查询结果列表
     */
    public static <T> java.util.List<T> executeNativeQueryInSchema(EntityManager entityManager, String schemaName, 
            String sql, Class<T> clazz) {
        try {
            // 获取数据库连接并设置schema
            Connection connection = entityManager.unwrap(Connection.class);
            String previousSchema = connection.getSchema();
            
            try {
                connection.setSchema(schemaName);
                return entityManager.createNativeQuery(sql, clazz).getResultList();
            } finally {
                // 恢复原来的schema
                if (previousSchema != null) {
                    connection.setSchema(previousSchema);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("在schema " + schemaName + "中执行查询失败: " + sql, e);
        }
    }
    
    /**
     * 在指定的schema中执行原生SQL更新，不改变当前线程的schema上下文
     * 
     * @param entityManager 实体管理器
     * @param schemaName schema名称
     * @param sql 原生SQL更新语句
     * @return 更新影响的行数
     */
    public static int executeNativeUpdateInSchema(EntityManager entityManager, String schemaName, String sql) {
        try {
            // 获取数据库连接并设置schema
            Connection connection = entityManager.unwrap(Connection.class);
            String previousSchema = connection.getSchema();
            
            try {
                connection.setSchema(schemaName);
                return entityManager.createNativeQuery(sql).executeUpdate();
            } finally {
                // 恢复原来的schema
                if (previousSchema != null) {
                    connection.setSchema(previousSchema);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("在schema " + schemaName + "中执行更新失败: " + sql, e);
        }
    }
    
    /**
     * 构造带schema前缀的表名
     * 
     * @param schemaName schema名称
     * @param tableName 表名
     * @return 带schema前缀的完整表名 (schema.table)
     */
    public static String qualifyTableName(String schemaName, String tableName) {
        return schemaName + "." + tableName;
    }
    
    /**
     * 构造public schema下的表名
     * 
     * @param tableName 表名
     * @return 带public schema前缀的完整表名 (public.table)
     */
    public static String qualifyPublicTableName(String tableName) {
        return qualifyTableName("public", tableName);
    }
    
    /**
     * 构造公司schema下的表名
     * 
     * @param schemaName 公司schema名称
     * @param tableName 表名
     * @return 带公司schema前缀的完整表名 (schema.table)
     */
    public static String qualifyCompanyTableName(String schemaName, String tableName) {
        return qualifyTableName(schemaName, tableName);
    }
}