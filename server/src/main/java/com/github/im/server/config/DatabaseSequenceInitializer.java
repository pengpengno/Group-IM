package com.github.im.server.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库序列初始化器
 * 用于修复因直接导入数据导致的序列值不正确问题
 */
@Slf4j
@Configuration
public class DatabaseSequenceInitializer {

    private final JdbcTemplate jdbcTemplate;
    private final SequenceSyncConfig sequenceSyncConfig;
    private final DataSource dataSource;

    public DatabaseSequenceInitializer(JdbcTemplate jdbcTemplate, SequenceSyncConfig sequenceSyncConfig, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.sequenceSyncConfig = sequenceSyncConfig;
        this.dataSource = dataSource;
    }

    @PostConstruct
    @Transactional
    public void init() {
        syncSequences();
    }
    
    /**
     * 同步所有配置的表序列
     */
    @Transactional
    public void syncSequences() {
        if (!sequenceSyncConfig.isEnabled()) {
            log.info("序列同步功能已禁用");
            return;
        }
        
        log.info("开始检查并修复数据库序列值");
        try {
            // 从DataSource获取数据库产品名称
            String databaseProductName = getDatabaseProductName();
            log.debug("检测到的数据库产品: {}", databaseProductName);
            
            List<SequenceSyncConfig.TableSequence> tables;
            if (sequenceSyncConfig.isAutoScan()) {
                // 自动扫描所有包含自增主键的表
                tables = scanTablesWithAutoIncrementColumns(databaseProductName);
                log.info("自动扫描到 {} 个包含自增主键的表", tables.size());
            } else {
                // 使用配置的表列表
                tables = sequenceSyncConfig.getTables();
                log.info("使用配置的 {} 个表", tables.size());
            }
            
            for (SequenceSyncConfig.TableSequence table : tables) {
                fixTableSequence(table.getTableName(), table.getPrimaryKeyColumn(), databaseProductName);
            }
            log.info("数据库序列值修复完成");
        } catch (Exception e) {
            log.error("修复数据库序列值时发生错误: {}", e.getMessage(), e);
            throw new RuntimeException("序列同步失败", e);
        }
    }

    /**
     * 从DataSource获取数据库产品名称
     * @return 数据库产品名称
     */
    private String getDatabaseProductName() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName().toLowerCase();
        } catch (SQLException e) {
            log.warn("无法获取数据库产品名称，使用默认值: {}", e.getMessage());
            return "unknown";
        }
    }
    
    /**
     * 扫描所有包含自增主键的表
     * @param databaseProductName 数据库产品名称
     * @return 包含自增主键的表列表
     */
    private List<SequenceSyncConfig.TableSequence> scanTablesWithAutoIncrementColumns(String databaseProductName) {
        List<SequenceSyncConfig.TableSequence> tables = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = connection.getCatalog();
            String schema = connection.getSchema();
            
            // 获取所有表
            try (ResultSet tablesRs = metaData.getTables(catalog, schema, null, new String[]{"TABLE"})) {
                while (tablesRs.next()) {
                    String tableName = tablesRs.getString("TABLE_NAME");
                    
                    // 检查表是否包含自增主键
                    SequenceSyncConfig.TableSequence tableSequence = getAutoIncrementPrimaryKey(metaData, catalog, schema, tableName, databaseProductName);
                    if (tableSequence != null) {
                        tables.add(tableSequence);
                        log.debug("发现包含自增主键的表: {}，主键列: {}", tableName, tableSequence.getPrimaryKeyColumn());
                    }
                }
            }
        } catch (SQLException e) {
            log.warn("扫描表时发生错误: {}", e.getMessage());
        }
        
        return tables;
    }
    
    /**
     * 获取表的自增主键列
     * @param metaData 数据库元数据
     * @param catalog 数据库目录
     * @param schema 数据库模式
     * @param tableName 表名
     * @param databaseProductName 数据库产品名称
     * @return 表序列信息，如果表不包含自增主键则返回null
     */
    private SequenceSyncConfig.TableSequence getAutoIncrementPrimaryKey(DatabaseMetaData metaData, 
                                                                       String catalog, String schema, 
                                                                       String tableName, 
                                                                       String databaseProductName) {
        try (ResultSet columnsRs = metaData.getColumns(catalog, schema, tableName, null)) {
            while (columnsRs.next()) {
                String columnName = columnsRs.getString("COLUMN_NAME");
                String isAutoIncrement = columnsRs.getString("IS_AUTOINCREMENT");
                
                // 检查是否是自增列
                if ("YES".equalsIgnoreCase(isAutoIncrement) || isAutoIncrementFlagSet(columnsRs, databaseProductName)) {
                    // 检查是否是主键
                    try (ResultSet primaryKeysRs = metaData.getPrimaryKeys(catalog, schema, tableName)) {
                        while (primaryKeysRs.next()) {
                            String pkColumnName = primaryKeysRs.getString("COLUMN_NAME");
                            if (columnName.equals(pkColumnName)) {
                                // 找到自增主键列
                                return new SequenceSyncConfig.TableSequence(tableName, columnName);
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            log.warn("检查表 {} 的列信息时发生错误: {}", tableName, e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 根据数据库类型判断是否是自增列
     * @param columnsRs 列信息结果集
     * @param databaseProductName 数据库产品名称
     * @return 是否是自增列
     * @throws SQLException SQL异常
     */
    private boolean isAutoIncrementFlagSet(ResultSet columnsRs, String databaseProductName) throws SQLException {
        if (databaseProductName.contains("postgresql")) {
            // PostgreSQL使用COLUMN_DEF检查nextval
            String columnDef = columnsRs.getString("COLUMN_DEF");
            return columnDef != null && columnDef.contains("nextval");
        } else if (databaseProductName.contains("mysql")) {
            // MySQL使用COLUMN_SIZE和DECIMAL_DIGITS检查
            int columnSize = columnsRs.getInt("COLUMN_SIZE");
            int decimalDigits = columnsRs.getInt("DECIMAL_DIGITS");
            // 这里可以添加更详细的检查逻辑
            return true; // MySQL的自增列通常能通过IS_AUTOINCREMENT识别
        }
        return false;
    }

    /**
     * 修复指定表的序列值
     * @param tableName 表名
     * @param primaryKeyColumn 主键列名
     * @param databaseProductName 数据库产品名称
     */
    private void fixTableSequence(String tableName, String primaryKeyColumn, String databaseProductName) {
        try {
            // 获取表中的最大主键值
            String sql = String.format("SELECT COALESCE(MAX(%s), 0) FROM %s", primaryKeyColumn, tableName);
            Long maxId = jdbcTemplate.queryForObject(sql, Long.class);
            
            log.debug("{}表中最大ID: {}", tableName, maxId);
            
            if (maxId != null && maxId > 0) {
                if (databaseProductName.contains("postgresql")) {
                    // 使用PostgreSQL特定方式更新序列
                    String sequenceNameSql = String.format(
                        "SELECT pg_get_serial_sequence('%s', '%s')", 
                        tableName, primaryKeyColumn);
                    String sequenceName = jdbcTemplate.queryForObject(sequenceNameSql, String.class);
                    
                    if (sequenceName != null) {
                        // 使用不同的方式调用setval函数来避免返回结果问题
                        String sequenceUpdateSql = "SELECT setval(?, ?)";
                        jdbcTemplate.execute(sequenceUpdateSql, (PreparedStatementCallback<Boolean>) ps -> {
                            ps.setString(1, sequenceName);
                            ps.setLong(2, maxId);
                            ps.execute();
                            return true;
                        });
                        log.info("PostgreSQL表{}序列已更新为: {}", tableName, maxId);
                    } else {
                        log.warn("无法获取PostgreSQL表{}的序列名称", tableName);
                    }
                } else if (databaseProductName.contains("mysql")) {
                    // MySQL使用AUTO_INCREMENT
                    String autoIncrementUpdateSql = String.format(
                        "ALTER TABLE %s AUTO_INCREMENT = ?", tableName);
                    jdbcTemplate.update(autoIncrementUpdateSql, maxId + 1);
                    log.info("MySQL表{} AUTO_INCREMENT已更新为: {}", tableName, maxId + 1);
                } else if (databaseProductName.contains("h2")) {
                    // H2数据库
                    String sequenceUpdateSql = String.format(
                        "ALTER SEQUENCE %s_%s_seq RESTART WITH ?", tableName, primaryKeyColumn);
                    jdbcTemplate.update(sequenceUpdateSql, maxId + 1);
                    log.info("H2表{}序列已更新为: {}", tableName, maxId + 1);
                } else {
                    log.info("当前数据库 {} 暂不支持自动序列更新", databaseProductName);
                }
            } else {
                log.info("{}表为空或无数据，无需更新序列", tableName);
            }
        } catch (Exception e) {
            log.warn("修复{}表序列时发生错误: {}", tableName, e.getMessage(), e);
            throw new RuntimeException("修复表" + tableName + "序列失败", e);
        }
    }
}